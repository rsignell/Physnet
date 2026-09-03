#!/usr/bin/env python3
"""
tex_to_html.py  –  Convert a Physnet module (nphmods.sty LaTeX) to an HTML fragment.

Usage:
    python scripts/tex_to_html.py <module_dir> <module_id>
    python scripts/tex_to_html.py /tmp/physnet_sample/m10 m10

Output: HTML written to stdout (redirect to a file or pipe into Astro).
"""

import re
import sys
import os
import html as html_lib


# ── Brace-parsing helpers ─────────────────────────────────────────────────────

def get_arg(text, pos):
    """Extract one balanced {…} argument at pos (skipping leading whitespace).
    Returns (content_string, new_pos).  Returns ('', pos) if no brace found."""
    n = len(text)
    while pos < n and text[pos] in ' \t\n':
        pos += 1
    if pos >= n or text[pos] != '{':
        return '', pos
    depth = 0
    i = pos
    while i < n:
        ch = text[i]
        if ch == '\\':
            i += 2          # skip escaped char
            continue
        if ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
            if depth == 0:
                return text[pos + 1 : i], i + 1
        i += 1
    return text[pos + 1 :], n   # unclosed brace – return rest


def get_n_args(text, pos, n):
    """Extract exactly n brace arguments, return (list_of_args, new_pos)."""
    args = []
    for _ in range(n):
        arg, pos = get_arg(text, pos)
        args.append(arg)
    return args, pos


def skip_opt_arg(text, pos):
    """Skip a single optional [...] argument (brace/bracket balanced) if the
    next non-space character is '['.  Returns the new position."""
    n = len(text)
    j = pos
    while j < n and text[j] in ' \t\n':
        j += 1
    if j >= n or text[j] != '[':
        return pos
    depth = 0
    while j < n:
        ch = text[j]
        if ch == '\\':
            j += 2
            continue
        if ch == '[':
            depth += 1
        elif ch == ']':
            depth -= 1
            if depth == 0:
                return j + 1
        j += 1
    return pos


def find_env_end(text, pos, env_name):
    """Find the matching \\end{env_name} starting from pos, respecting nesting.
    Returns (content, pos_after_end)."""
    begin_pat = re.compile(r'\\begin\s*\{' + re.escape(env_name) + r'\}')
    end_pat   = re.compile(r'\\end\s*\{'   + re.escape(env_name) + r'\}')
    depth = 1
    i = pos
    while i < len(text):
        mb = begin_pat.search(text, i)
        me = end_pat.search(text, i)
        if me is None:
            break
        if mb is not None and mb.start() < me.start():
            depth += 1
            i = mb.end()
        else:
            depth -= 1
            if depth == 0:
                return text[pos : me.start()], me.end()
            i = me.end()
    return text[pos:], len(text)


# ── Math content post-processing ──────────────────────────────────────────────

def convert_math(s):
    """Translate Physnet math macros to standard LaTeX for KaTeX."""
    # magnitude first (its argument may itself contain \vect)
    s = re.sub(r'\\vecmag\{((?:[^{}]|\{[^{}]*\})*)\}', r'\\left|\1\\right|', s)
    s = re.sub(r'\\vect?prime\{([^{}]*)\}', r"{\\vec{\1}}'", s)
    s = re.sub(r'\\ve[cx]t?\{([^{}]*)\}', r'\\vec{\1}', s)   # \vect \vex \vec
    s = re.sub(r'\\uve[cx]\{([^{}]*)\}', r'\\hat{\1}', s)    # \uvec \uvex
    # \unit{…} may contain a nested \up{N} exponent; render the unit name in
    # upright text with the exponent kept *outside* \text{} (KaTeX rejects a
    # bare ^ inside \text{}).
    def _unit(m):
        body = m.group(1)
        body = re.sub(r'\\up\{([^{}]*)\}', r'}^{\1}\\text{', body)
        return r'\,\text{' + body + '}'
    s = re.sub(r'\\unit\{((?:[^{}]|\{[^{}]*\})*)\}', _unit, s)
    s = re.sub(r'\\up\{([^{}]*)\}',   r'^{\1}',     s)
    s = s.replace(r'\degrees', r'^\circ')
    s = s.replace(r'\degreesC', r'^{\circ}\,\text{C}')
    s = s.replace(r'\degreesF', r'^{\circ}\,\text{F}')
    s = s.replace(r'\AA', r'\text{Å}')
    s = re.sub(r'\\[dt]?frac(?![A-Za-z])', r'\\frac', s)
    s = re.sub(r'\\fract(?![A-Za-z])', r'\\frac', s)
    # Text macros that sometimes leak inside \m{}: drop the wrapper.
    s = re.sub(r'\\Index\{(?:[^{}]|\{[^{}]*\})*\}', '', s)
    s = re.sub(r'\\(?:Emph|Quote|textit|textbf|Term)\{((?:[^{}]|\{[^{}]*\})*)\}',
               r'\1', s)
    # MISN shorthands KaTeX doesn't know
    s = re.sub(r'\\ds\b', r'\\displaystyle ', s)
    s = re.sub(r'\\OverArc\{([^{}]*)\}', r'\\overset{\\frown}{\1}', s)
    s = re.sub(r'\\Overline\{([^{}]*)\}', r'\\overline{\1}', s)
    s = re.sub(r'\\text\{([^{}]*)\}', r'\\text{\1}', s)   # passthrough
    return s


# ── Main converter class ──────────────────────────────────────────────────────

class PhysnetConverter:

    def __init__(self, module_id, figures_path='figures'):
        self.module_id    = module_id
        self.figures_path = figures_path   # relative URL prefix for figure images
        self.footnotes    = []             # list of (label, html)

    # ── Public entry point ────────────────────────────────────────────────────

    def convert(self, text):
        # Source files from the Windows CorelDRAW/LaTeX toolchain are CRLF;
        # normalise so stray \r never reaches the output or a regex.
        text = text.replace('\r\n', '\n').replace('\r', '\n')
        text = self._strip_comments(text)
        text = self._strip_preamble(text)
        html = self._process(text)
        # A superscript [S-N] hint marker is a footnote-style reference: it
        # should hug the word or punctuation it follows, not float after a
        # space (the source usually has "... be: \help{4}").
        html = re.sub(r'[ \t\n]+(<sup class="help-link">)', r'\1', html)
        return html

    # ── Pre-processing ────────────────────────────────────────────────────────

    def _strip_comments(self, text):
        out = []
        i = 0
        while i < len(text):
            if text[i] == '\\' and i + 1 < len(text):
                out.append(text[i : i + 2])
                i += 2
            elif text[i] == '%':
                while i < len(text) and text[i] != '\n':
                    i += 1
            else:
                out.append(text[i])
                i += 1
        return ''.join(out)

    def _strip_preamble(self, text):
        """Strip \revhist{…} and other file-level preamble macros."""
        text = re.sub(r'\\revhist\s*\{[^}]*\}', '', text)
        text = re.sub(r'\\defmodlength\s*\{[^}]*\}', '', text)
        # table-of-contents entries
        text = re.sub(r'\\tcsc\s*\{[^}]*\}\s*\{[^}]*\}', '', text)
        text = re.sub(r'\\tcpc\s*\{[^}]*\}\s*\{[^}]*\}', '', text)
        # Stray group-close annotations: the source convention is "}% /par",
        # "}% /Sect" etc.; where the author dropped the "%" the "/par" leaks
        # into the text as literal characters (present in the official PDF
        # too).  Drop these bare markers.
        text = re.sub(r'(?<![A-Za-z/])/(?:par|Sect|SubSect|SubSubSect)\b', '', text)
        # LaTeX box save/restore used to defer a fragment (usually a \help{}
        # link) into an equation.  We cannot honour it, so drop the plumbing;
        # \usebox expands to nothing (the deferred fragment is lost).
        _grp = r'\{(?:[^{}]|\{[^{}]*\})*\}'
        text = re.sub(r'\\newsavebox\s*' + _grp, '', text)
        text = re.sub(r'\\s(?:box|avebox)\s*' + _grp + r'\s*' + _grp, '', text)
        text = re.sub(r'\\usebox\s*' + _grp, '', text)
        return text

    # ── Core recursive processor ──────────────────────────────────────────────

    def _process(self, text):
        """Walk through text, dispatch on backslash macros and environments."""
        out  = []
        i    = 0
        n    = len(text)

        while i < n:
            ch = text[i]

            # ── Backslash ──────────────────────────────────────────────────
            if ch == '\\':
                i += 1
                if i >= n:
                    break

                # Escaped special chars
                if text[i] in r'{}%$&#_':
                    specials = {'{': '{', '}': '}', '%': '%', '$': '$',
                                '&': '&amp;', '#': '#', '_': '_'}
                    out.append(specials.get(text[i], text[i]))
                    i += 1
                    continue
                if text[i] == '\\':
                    out.append('<br>')
                    i += 1
                    continue
                if text[i] == '~':
                    out.append('&nbsp;')
                    i += 1
                    continue
                if text[i] == ',':          # thin space
                    out.append('&thinsp;')
                    i += 1
                    continue
                if text[i] in ';:!':        # \; \: \! math spacing in text
                    i += 1
                    continue

                # Accents.  Symbol accents (\'e \`a \"o \^i \=o \.z) may take
                # the base letter immediately; letter-name accents (\c{c},
                # \v{s}, \u{g}) need a brace or a space so we don't eat the
                # tail of \vec, \check, \dot, ...
                _comb = {"'": '́', '`': '̀', '"': '̈', '^': '̂', '=': '̄',
                         '.': '̇', 'c': '̧', 'v': '̌', 'u': '̆', 'H': '̋',
                         'k': '̨', 'r': '̊', 'd': '̣', 'b': '̱'}
                if text[i] in _comb:
                    acc = text[i]
                    j = i + 1
                    had_space = False
                    while j < n and text[j] in ' \t':
                        j += 1
                        had_space = True
                    base = ''
                    if j < n and text[j] == '{':
                        base, j = get_arg(text, j)
                    elif j < n and text[j].isalpha() and (
                            acc in "'`\"^=." or had_space):
                        base, j = text[j], j + 1
                    if base:
                        import unicodedata as _ud
                        out.append(_ud.normalize('NFC', base[:1] + _comb[acc] + base[1:]))
                        i = j
                        continue

                # Macro name
                j = i
                while j < n and (text[j].isalpha() or text[j] == '*'):
                    j += 1
                name = text[i:j]
                i    = j

                # Dispatch
                html_frag, i = self._dispatch(name, text, i)
                out.append(html_frag)

            # ── Dollar-sign math ───────────────────────────────────────────
            elif ch == '$':
                if i + 1 < n and text[i + 1] == '$':
                    # Display math $$...$$
                    end = text.find('$$', i + 2)
                    if end == -1:
                        end = n - 2
                    eq = convert_math(text[i + 2 : end])
                    out.append(f'\\[{eq}\\]')
                    i = end + 2
                else:
                    # Inline math $...$
                    end = text.find('$', i + 1)
                    if end == -1:
                        end = n - 1
                    eq = convert_math(text[i + 1 : end])
                    out.append(f'\\({eq}\\)')
                    i = end + 1

            # ── Non-breaking space ─────────────────────────────────────────
            elif ch == '~':
                out.append('&nbsp;')
                i += 1

            # ── Paragraph break ────────────────────────────────────────────
            elif ch == '\n' and i + 1 < n and text[i + 1] == '\n':
                out.append('\n<p>')
                # Consume all blank lines
                while i < n and text[i] == '\n':
                    i += 1

            # ── Bare braces (group delimiters) ─────────────────────────────
            elif ch == '{':
                content, i = get_arg(text, i)
                inner = self._process(content)
                out.append(inner)

            elif ch == '}':
                # Stray closing brace – skip
                i += 1

            else:
                out.append(ch)
                i += 1

        return ''.join(out)

    # ── Macro dispatcher ──────────────────────────────────────────────────────

    def _dispatch(self, name, text, i):
        """Handle one macro. Return (html_fragment, new_pos)."""

        # Empty name: backslash followed by non-alpha (digit, space, etc.) — skip it
        if not name:
            return '', i

        # ── Inline math ──
        if name == 'm':
            arg, i = get_arg(text, i)
            # Some sources mis-tag a multi-line derivation as inline \m{},
            # sticking \medskip\newline between the "=" lines -- invalid in
            # KaTeX, so the whole \(...\) leaks as raw text.  Promote such a
            # block to a display \begin{aligned} instead.
            if re.search(r'\\(?:newline|medskip|smallskip|bigskip)\b', arg):
                arg = re.sub(r'\\(?:medskip|smallskip|bigskip)\b', '', arg)
                arg = re.sub(r'\\newline\b', r' \\\\ ', arg)
                return (f'\\[\\begin{{aligned}}{convert_math(arg)}'
                        f'\\end{{aligned}}\\]'), i
            return f'\\({convert_math(arg)}\\)', i

        if name == 'vect':
            arg, i = get_arg(text, i)
            return f'\\(\\vec{{{convert_math(arg)}}}\\)', i

        if name == 'uvec':
            arg, i = get_arg(text, i)
            return f'\\(\\hat{{{arg}}}\\)', i

        # ── Superscript / units / degrees ──
        if name == 'up':
            arg, i = get_arg(text, i)
            return f'<sup>{arg}</sup>', i

        if name == 'unit':
            arg, i = get_arg(text, i)
            return f'<span class="unit">&thinsp;{self._process(arg)}</span>', i

        if name == 'degrees':
            return '°', i

        # ── Text formatting ──
        if name in ('textbf', 'mathbf'):
            arg, i = get_arg(text, i)
            return f'<strong>{self._process(arg)}</strong>', i

        if name in ('textit', 'emph', 'textsl'):
            arg, i = get_arg(text, i)
            return f'<em>{self._process(arg)}</em>', i

        if name == 'Quote':
            arg, i = get_arg(text, i)
            return f'\u201c{self._process(arg)}\u201d', i

        if name == 'Index':
            _, i = get_arg(text, i)    # discard index entries
            return '', i

        # ── Display math ──
        if name == 'Eqn':
            label, i = get_arg(text, i)
            eq, i    = get_arg(text, i)
            math = convert_math(eq)
            lbl = label.strip()
            if lbl:
                return (f'<span class="eqn-block" id="eqn-{lbl}">'
                        f'\\[{math}\\]'
                        f'<span class="eqn-number">({lbl})</span></span>'), i
            return f'\\[{math}\\]', i

        if name == 'FiveEqns':
            # \FiveEqns{eqnum}{row1}...{row5} -- five 2-column aligned rows
            num, i = get_arg(text, i)
            rows = []
            for _ in range(5):
                r, i = get_arg(text, i)
                if r.strip():
                    rows.append(r.strip())
            math = convert_math(' \\\\ '.join(rows))
            block = f'\\[\\begin{{aligned}}{math}\\end{{aligned}}\\]'
            lbl = num.strip()
            if lbl:
                return (f'<span class="eqn-block" id="eqn-{lbl}">{block}'
                        f'<span class="eqn-number">({lbl})</span></span>'), i
            return block, i

        # ── Footnotes ──
        if name == 'Footnote':
            num,  i = get_arg(text, i)
            body, i = get_arg(text, i)
            fn_html = self._process(body)
            self.footnotes.append((num, fn_html))
            return (f'<sup class="fn-ref"><a href="#fn-{num}" id="fnref-{num}">'
                    f'{num}</a></sup>'), i

        # ── Cross-references ──
        if name == 'Figref':
            num, i = get_arg(text, i)
            return f'<a href="#fig-{num}" class="fig-ref">Fig.&nbsp;{num}</a>', i

        if name in ('Figsref', 'Figureref'):
            num, i = get_arg(text, i)
            return f'<a href="#fig-{num}" class="fig-ref">Fig.&nbsp;{num}</a>', i

        if name == 'Figssref':
            num, i = get_arg(text, i)
            return f'<a href="#fig-{num}" class="fig-ref">Fig.&nbsp;{num}</a>', i

        if name in ('Eqnref', 'Eqref'):
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eq.&nbsp;({num})</a>', i

        if name == 'EqnXref':
            ref, i = get_arg(text, i)
            return f'Eq.&nbsp;({ref})', i

        if name == 'Eqnsref':
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eqs.&nbsp;({num})</a>', i

        if name == 'AsSect':
            num, i = get_arg(text, i)
            return f'<a href="#as-{num}" class="as-ref">AS&nbsp;{num}</a>', i

        if name == 'help':
            # Printed module renders this as a small superscript [S-N] pointing
            # to Sequence [S-N] in the Special Assistance Supplement.
            num, i = get_arg(text, i)
            return (f'<sup class="help-link"><a href="#help-{num}" '
                    f'title="see Sequence [S-{num}] in the Special Assistance '
                    f'Supplement">[S-{num}]</a></sup>'), i

        if name == 'parenhelp':
            num, i = get_arg(text, i)
            return (f'<a href="#help-{num}" class="help-link help-link-inline" '
                    f'title="see Sequence [S-{num}] in the Special Assistance '
                    f'Supplement">[S-{num}]</a>'), i

        if name in ('prrqone', 'prrqtwo'):
            ref, i = get_arg(text, i)
            # ref like "0-14" → module number is the last segment
            mid = ref.split('-')[-1]
            return (f'<a href="../m{mid}/" class="module-ref">'
                    f'MISN-0-{mid}</a>'), i

        # ── Reif-style section structure (older module format) ──
        if name == 'SubSect':
            i = skip_opt_arg(text, i)          # [\Index{...}] entries
            title, i = get_arg(text, i)
            # \SubSect{title}{body} -- body is a second braced group
            body = ''
            j = i
            while j < len(text) and text[j] in ' \t\n':
                j += 1
            if j < len(text) and text[j] == '%':
                while j < len(text) and text[j] != '\n':
                    j += 1
                while j < len(text) and text[j] in ' \t\n':
                    j += 1
            if j < len(text) and text[j] == '{':
                body, i = get_arg(text, j)
            title_html = self._process(title)
            sid = re.sub(r'\W+', '-', title.lower()).strip('-')
            body_html = self._process(body) if body else ''
            return (f'<h3 id="subsect-{sid}" class="subsect-title">'
                    f'{title_html}</h3>\n{body_html}\n'), i

        if name == 'xpSubSubSect':
            num, i = get_arg(text, i)
            letter, i = get_arg(text, i)
            title, i = get_arg(text, i)
            title_html = self._process(title)
            sid = f'{num}{letter}'
            return f'<h3 id="pcap-{sid}">{num}{letter}. {title_html}</h3>\n', i

        if name in ('TxtFigureRef', 'TxtFigRef'):
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="fig-ref">Fig.&nbsp;{sec}{num}</span>', i

        if name == 'TxtSectRef':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="sect-ref">Sect.&nbsp;{sec}{num}</span>', i

        if name in ('TxtEqnRef',):
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="eqn-ref">Eq.&nbsp;({sec}{num})</span>', i

        if name == 'FullFigure':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            cap, i = get_arg(text, i)
            fname, i = get_arg(text, i)
            return self._reif_fig(f'{sec}{num}', self._process(cap), fname), i

        if name in ('TxtProb',):
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            title, i = get_arg(text, i)
            title_html = self._process(title)
            pid = f'{sec}{num}'
            return f'<div class="problem" id="prob-{pid}"><p><strong>Problem&nbsp;{pid}: {title_html}</strong>\n', i

        if name in ('ProbAns',):
            content, i = get_arg(text, i)
            return f'<p class="prob-ans"><em>Answer:</em> {self._process(content)}</p>\n', i

        if name in ('AnsRef',):
            ref, i = get_arg(text, i)
            return f'<span class="ans-ref">[{ref}]</span>', i

        if name in ('TxtHelp',):
            content, i = get_arg(text, i)
            return f'<span class="help-ref">{self._process(content)}</span>', i

        if name in ('TxtCapPrac', 'TxtCapPra'):
            content, i = get_arg(text, i)
            return (f'<p class="txt-prac"><em>Practice:</em> '
                    f'{self._process(content)}</p>\n'), i

        if name == 'SummaryItem':
            head, i = get_arg(text, i)
            return f'<h4 class="summary-head">{self._process(head)}</h4>\n', i

        if name == 'DisplayEqn':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            eq, i = get_arg(text, i)
            eid = f'{sec}{num}'.strip()
            block = f'\\[{convert_math(eq)}\\]'
            if eid:
                return (f'<span class="eqn-block" id="eqn-{eid}">{block}'
                        f'<span class="eqn-number">({eid})</span></span>'), i
            return f'<div class="display-eqn">{block}</div>\n', i

        if name == 'UnframedFigure':
            a1, i = get_arg(text, i)
            # 1-arg form \UnframedFigure{filename}, or the 4-arg
            # \UnframedFigure{sec}{num}{cap}{filename}
            if re.match(r'[A-Za-z]*\d+gr\d', a1) or a1.endswith('.eps'):
                return self._reif_fig('', '', a1), i
            num, i = get_arg(text, i)
            cap, i = get_arg(text, i)
            fname, i = get_arg(text, i)
            return self._reif_fig(f'{a1}{num}', self._process(cap), fname), i

        if name == 'LeftFigure':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            cap, i = get_arg(text, i)
            fname, i = get_arg(text, i)
            return self._reif_fig(f'{sec}{num}', self._process(cap), fname,
                                  extra_cls=' module-figure-left'), i

        if name == 'TwoFigures':
            # \TwoFigures{sec}{num1}{cap1}{file1}{sec}{num2}{cap2}{file2}
            args, i = get_n_args(text, i, 8)
            sec1, num1, cap1, f1, sec2, num2, cap2, f2 = args
            a = self._reif_fig(f'{sec1}{num1}', self._process(cap1), f1)
            b = self._reif_fig(f'{sec2}{num2}', self._process(cap2), f2)
            return f'<div class="two-figures">{a}{b}</div>\n', i

        if name == 'ThreeFigures':
            # {s}{n}{f} x3, no captions
            (s1, n1, f1, s2, n2, f2, s3, n3, f3), i = get_n_args(text, i, 9)
            parts = ''.join(self._reif_fig(f'{s}{n}', '', f)
                            for s, n, f in ((s1, n1, f1), (s2, n2, f2), (s3, n3, f3)))
            return f'<div class="two-figures">{parts}</div>\n', i

        if name == 'TableAndFigure':
            # {tsec}{tnum}{tcap}{tabular}{fsec}{fnum}{fcap}{fname}
            (tsec, tnum, tcap, tbody, fsec, fnum, fcap, fname), i = get_n_args(text, i, 8)
            tbl = self._process(tbody)
            fig = self._reif_fig(f'{fsec}{fnum}', self._process(fcap), fname)
            return (f'<div class="two-figures">'
                    f'<figure class="module-figure"><figcaption>'
                    f'<strong>Table&nbsp;{tsec}-{tnum}.</strong> '
                    f'{self._process(tcap)}</figcaption>{tbl}</figure>'
                    f'{fig}</div>\n'), i

        if name in ('SugFrameRef', 'PraFrameRef'):
            ref, i = get_arg(text, i)
            label = 'Suggestion' if name == 'SugFrameRef' else 'Practice'
            return f'<span class="frame-ref">[{label}&nbsp;{ref}]</span>', i

        if name == 'ProbHead':
            title, i = get_arg(text, i)
            # optional second arg (section letter)
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return f'<p><strong>{self._process(title)}</strong></p>\n', i

        if name == 'SubSectTitle':
            title, i = get_arg(text, i)
            sid = re.sub(r'\W+', '-', title.lower()).strip('-')
            return f'<h3 id="ss-{sid}">{self._process(title)}</h3>\n', i

        if name == 'TxtRelRef':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="eqn-ref">Eq.&nbsp;({sec}{num})</span>', i

        if name in ('MinorEqn',):
            eq, i = get_arg(text, i)
            return f'\\[{convert_math(eq)}\\]\n', i

        if name == 'MajorDisplayEqn':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            eq, i = get_arg(text, i)
            eid = f'{sec}{num}'
            return f'<div class="display-eqn" id="eqn-{eid}">\\[{convert_math(eq)}\\]</div>\n', i

        if name in ('boldm',):
            content, i = get_arg(text, i)
            return f'<strong>\\({convert_math(content)}\\)</strong>', i

        if name in ('TxtPrac',):
            content, i = get_arg(text, i)
            return f'<div class="practice-note">{self._process(content)}</div>\n', i

        if name == 'TxtDefinition':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            term, i = get_arg(text, i)
            defn, i = get_arg(text, i)
            did = f'{sec}{num}'
            return (f'<div class="definition" id="def-{did}">'
                    f'<strong>Definition&nbsp;{did}: {self._process(term)}</strong> — '
                    f'{self._process(defn)}</div>\n'), i

        if name == 'TxtDefRef':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="def-ref">Defn.&nbsp;{sec}{num}</span>', i

        if name in ('TxtRuleRef', 'TxtStaRef', 'TxtStatementRef'):
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            word = 'Rule' if name == 'TxtRuleRef' else 'Statement'
            return f'<span class="def-ref">{word}&nbsp;{sec}{num}</span>', i

        # Cross-references into the Reif textbook (chapter-numbered)
        if name == 'ChRef':
            ch, i = get_arg(text, i)
            return f'<span class="ch-ref">Ch.&nbsp;{ch}</span>', i
        if name == 'ChRefNo':
            ch, i = get_arg(text, i)
            return ch, i
        if name in ('TutSectRef',):
            sec, i = get_arg(text, i)
            return f'<span class="sect-ref">Tutorial&nbsp;Sect.&nbsp;{sec}</span>', i
        if name in ('TxtSectChRef',):
            ch, i = get_arg(text, i)
            sec, i = get_arg(text, i)
            return f'<span class="sect-ref">Sect.&nbsp;{sec} of Ch.&nbsp;{ch}</span>', i
        if name in ('TxtEqnChRef',):
            ch, i = get_arg(text, i)
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="eqn-ref">Eq.&nbsp;({sec}-{num}) of Ch.&nbsp;{ch}</span>', i
        if name in ('TxtEqnsRef', 'TxtEqnssRef'):
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="eqn-ref">Eqs.&nbsp;({sec}-{num})</span>', i

        if name == 'Order':
            content, i = get_arg(text, i)
            return (f'<p class="order-note"><strong>&rarr;</strong> '
                    f'{self._process(content)}</p>\n'), i

        if name == 'TxtExample':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            title, i = get_arg(text, i)
            body, i = get_arg(text, i)          # \TxtExample{s}{n}{title}{body}
            eid = f'{sec}{num}'
            return (f'<div class="example" id="ex-{eid}">'
                    f'<p><strong>Example&nbsp;{eid}: {self._process(title)}</strong> '
                    f'{self._process(body)}</div>\n'), i

        if name == 'TxtHelpTwo':
            h1, i = get_arg(text, i)
            h2, i = get_arg(text, i)
            return (f'<span class="help-refs">{self._process(h1)} {self._process(h2)}</span>'), i

        if name in ('MeSuppl',):
            _, i = get_arg(text, i)
            return '', i

        if name in ('TxtRule',):
            return '<hr>\n', i

        if name in ('TxtAdvice',):
            content, i = get_arg(text, i)
            return f'<div class="advice-box">{self._process(content)}</div>\n', i

        if name in ('unitname',):
            content, i = get_arg(text, i)
            return f'<em>{self._process(content)}</em>', i

        # ── Reif-style cross-refs (Table) ──
        if name == 'Table':
            cols, i = get_arg(text, i)   # number of columns
            fmt, i = get_arg(text, i)    # column format
            caption, i = get_arg(text, i)
            cap_html = self._process(caption)
            return f'<div class="table-caption"><strong>{cap_html}</strong></div>\n', i

        # ── Particle symbols ──
        if name == 'lel':
            return '<em class="lepton">e</em>', i

        if name == 'ccon':
            return 'c', i

        if name == 'lcr':
            # \lcr{left}{center}{right} — three-column layout, use center
            left, i = get_arg(text, i)
            center, i = get_arg(text, i)
            right, i = get_arg(text, i)
            parts_html = []
            for part in (left, center, right):
                h = self._process(part)
                if h.strip():
                    parts_html.append(h)
            return ' '.join(parts_html), i

        # ── Section structure ──
        if name == 'Sect':
            return self._sect(text, i)

        if name in ('pcap', 'xpcap'):
            i = skip_opt_arg(text, i)   # optional [\Index{…}] entries
            (sec, letter, title), i = get_n_args(text, i, 3)
            title_html = self._process(title)
            pid = f'pcap-{sec}-{letter}'
            return f'<h3 id="{pid}">{sec}{letter}. {title_html}</h3>\n', i

        if name == 'icap':
            title, i = get_arg(text, i)
            return f'<p class="icap"><strong>{self._process(title)}:</strong></p>\n', i

        # \scap{num}{TITLE} — section caption (older format), like \Sect
        if name in ('scap', 'spcap'):
            num, i = get_arg(text, i)
            title, i = get_arg(text, i)
            title_html = self._process(title)
            sid = f'sect-{num}'
            return f'<section id="{sid}" class="module-section section-text"><h2 id="{sid}">{num}. {title_html}</h2>\n', i

        # \eqn{label}{math} — numbered/labeled equation (older format)
        if name == 'eqn':
            label, i = get_arg(text, i)
            eq, i = get_arg(text, i)
            eq_math = convert_math(eq)
            if label.strip():
                return f'<div class="eqn-block" id="eqn-{label}">\\[{eq_math}\\]</div>\n', i
            return f'\\[{eq_math}\\]\n', i

        if name == 'SubSubSectTitle':
            title, i = get_arg(text, i)
            sid = re.sub(r'\W+', '-', title.lower()).strip('-')
            return f'<h4 id="sss-{sid}">{self._process(title)}</h4>\n', i

        # \fract{num}{den} — fraction (older alternative to \frac)
        if name == 'fract':
            num, i = get_arg(text, i)
            den, i = get_arg(text, i)
            return f'\\(\\frac{{{convert_math(num)}}}{{{convert_math(den)}}}\\)', i

        # \mTitle{title} \mAuthor{author} — metadata macros in body text (ignore)
        if name in ('mTitle', 'mAuthor'):
            _, i = get_arg(text, i)
            return '', i

        # \endinput / \TxStart — document delimiters
        if name in ('endinput', 'TxStart', 'TxEnd'):
            return '', i

        # \ChapterFirstPage{title}{outline}{intro} — Reif chapter opener
        if name == 'ChapterFirstPage':
            title, i = get_arg(text, i)
            outline, i = get_arg(text, i)
            intro, i = get_arg(text, i)
            title_html = self._process(title)
            outline_html = self._process(outline)
            intro_html = self._process(intro)
            return f'<h1>{title_html}</h1>\n<div class="chapter-outline">{outline_html}</div>\n<p>{intro_html}</p>', i

        # \index{...} — index entry, suppress
        if name == 'index':
            _, i = get_arg(text, i)
            # May have second arg {actual}{sort} form
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return '', i

        # \mathrm{...} in text context — just render content
        if name == 'mathrm':
            content, i = get_arg(text, i)
            return self._process(content), i

        # \imath — dotless i (math symbol)
        if name == 'imath':
            return '\\(\\imath\\)', i

        # Listing macros (m253 uses listings package for BASIC/Fortran code)
        if name == 'lstset':
            _, i = get_arg(text, i)
            return '', i

        if name == 'lstinputlisting':
            # optional [options] then {filename}
            if i < len(text) and text[i] == '[':
                end = text.find(']', i)
                i = end + 1 if end != -1 else i
            fname, i = get_arg(text, i)
            return f'<pre class="code-listing"><em>[Listing: {fname}]</em></pre>\n', i

        if name == 'lstinline':
            content, i = get_arg(text, i)
            return f'<code>{html_lib.escape(content)}</code>', i

        # \mbox{text} — text in math mode, treat as plain text
        if name == 'mbox':
            content, i = get_arg(text, i)
            return self._process(content), i

        # \CaptionAfterFigure — same as CaptionAfterFullFramedFigure
        if name == 'CaptionAfterFigure':
            cap, i = get_arg(text, i)
            fname, i = get_arg(text, i)
            cap_html = self._process(cap)
            return (f'<figure class="fig-centered">'
                    f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="" class="physnet-fig">'
                    f'<figcaption>{cap_html}</figcaption>'
                    f'</figure>\n'), i

        # \hs — horizontal space (thin space in math)
        if name == 'hs':
            return '\\;', i

        # \rem{...} — revision history comment, suppress
        if name == 'rem':
            _, i = get_arg(text, i)
            return '', i

        # \SectTitle{label}{TITLE} — section title (older format)
        if name == 'SectTitle':
            i = skip_opt_arg(text, i)          # [\Index{...}] entries
            label, i = get_arg(text, i)
            title, i = get_arg(text, i)
            title_html = self._process(title)
            sid = f'sect-{label}'
            lead = f'{label}. ' if label.strip() else ''
            return (f'<section id="{sid}" class="module-section section-text">'
                    f'<h2 id="{sid}">{lead}{title_html}</h2>\n'), i

        # \TxtTwoDisplayEqns{label}{lhs1}{rhs1}{lhs2}{rhs2}
        if name == 'TxtTwoDisplayEqns':
            label, i = get_arg(text, i)
            lhs1, i = get_arg(text, i)
            rhs1, i = get_arg(text, i)
            lhs2, i = get_arg(text, i)
            rhs2, i = get_arg(text, i)
            eid = label
            eq = (f'\\begin{{aligned}}{convert_math(lhs1)} &= {convert_math(rhs1)} \\\\'
                  f'{convert_math(lhs2)} &= {convert_math(rhs2)}\\end{{aligned}}')
            return f'<div class="eqn-block" id="eqn-{eid}">\\[{eq}\\]</div>\n', i

        # \eqref{label} — standard LaTeX equation reference
        if name == 'eqref':
            ref, i = get_arg(text, i)
            return f'<span class="eqn-ref">({ref})</span>', i

        # \TxtDefEqnStaRef{sec}{num} — definition/equation/statement reference
        if name == 'TxtDefEqnStaRef':
            sec, i = get_arg(text, i)
            num, i = get_arg(text, i)
            return f'<span class="def-ref">({sec}{num})</span>', i

        if name == 'TxtStatement':
            # \TxtStatement{sec}{num}{text} -- a highlighted rule/statement
            (sec, num, body), i = get_n_args(text, i, 3)
            tag = f'{sec}{num}'.strip()
            label = (f'<span class="statement-label">Statement&nbsp;{tag}:</span> '
                     if tag else '')
            return (f'<div class="statement">{label}{self._process(body)}</div>\n'), i

        if name == 'SubSubSect':
            (_id, title, content), i = get_n_args(text, i, 3)
            inner = self._process(content)
            return (f'<div class="example">'
                    f'<p class="example-label"><strong>{self._process(title)}</strong></p>'
                    f'{inner}</div>\n'), i

        if name == 'tryit':
            return '<span class="tryit">Try it:</span> ', i

        if name == 'dotfill':
            return '<span class="dotfill">&thinsp;·····&thinsp;</span>', i

        # ── Figures ──
        if name == 'ThreeCaptionedFramedFigures':
            return self._figures(text, i, 3)

        if name == 'TwoCaptionedFramedFigures':
            return self._figures(text, i, 2)

        if name == 'CaptionedFullFramedFigure':
            return self._figures(text, i, 1, full=True)

        if name == 'CaptionedLeftFramedFigure':
            # args: {fig_num}{caption}{filename}
            num,   i = get_arg(text, i)
            cap,   i = get_arg(text, i)
            fname, i = get_arg(text, i)
            cap_html = self._process(cap)
            return (f'<figure id="fig-{num}" class="fig-left-framed">'
                    f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="Figure {num}" class="physnet-fig">'
                    f'<figcaption>Fig.&nbsp;{num}. {cap_html}</figcaption>'
                    f'</figure>\n'), i

        if name == 'CenteredUnframedFixedFigure':
            # args: {filename} optional {caption}
            fname, i = get_arg(text, i)
            fname = re.sub(r'\.(eps|ps|pdf|png|jpg|jpeg)$', '', fname.strip())
            # peek for optional caption arg
            j = i
            while j < len(text) and text[j] in ' \t\n':
                j += 1
            if j < len(text) and text[j] == '{':
                cap, i = get_arg(text, i)
                cap_html = self._process(cap)
                return (f'<figure class="fig-centered">'
                        f'<img src="{self.figures_path}/{fname}.svg" '
                        f'alt="" class="physnet-fig">'
                        f'<figcaption>{cap_html}</figcaption>'
                        f'</figure>\n'), i
            else:
                return (f'<figure class="fig-centered">'
                        f'<img src="{self.figures_path}/{fname}.svg" '
                        f'alt="" class="physnet-fig">'
                        f'</figure>\n'), i

        if name in ('CaptionAfterFullFramedFigure', 'CaptionAfterLeftFigure',
                    'CaptionAfterFullUnframedFigure'):
            cap, i = get_arg(text, i)
            fname, i = get_arg(text, i)
            cap_html = self._process(cap)
            return (f'<figure class="fig-centered">'
                    f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="" class="physnet-fig">'
                    f'<figcaption>{cap_html}</figcaption>'
                    f'</figure>\n'), i

        if name == 'CharacterUnframedFigure':
            fname, i = get_arg(text, i)
            return (f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="" class="fig-inline">'), i

        if name == 'ItemFigure':
            content, i = get_arg(text, i)
            fname,   i = get_arg(text, i)
            inner = self._process(content)
            return (f'<span class="item-text">{inner}</span>'
                    f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="" class="fig-item">'), i

        if name == 'TwoEqns':
            _label, i = get_arg(text, i)
            eq1, i    = get_arg(text, i)
            eq2, i    = get_arg(text, i)
            return (f'\\[\\begin{{aligned}}{convert_math(eq1)} \\\\ '
                    f'{convert_math(eq2)}\\end{{aligned}}\\]\n'), i

        if name == 'ThreeEqns':
            _label, i = get_arg(text, i)
            eq1, i    = get_arg(text, i)
            eq2, i    = get_arg(text, i)
            eq3, i    = get_arg(text, i)
            return (f'\\[\\begin{{aligned}}{convert_math(eq1)} \\\\ '
                    f'{convert_math(eq2)} \\\\ '
                    f'{convert_math(eq3)}\\end{{aligned}}\\]\n'), i

        # ── Standard math macros appearing outside $ (pass through to KaTeX) ──
        _MATH_NO_ARGS = {
            'sin','cos','tan','sec','csc','cot',
            'sinh','cosh','tanh','log','ln','exp',
            'max','min','sup','inf','lim','det','deg',
            'sum','prod','int','oint','iint',
            'times','cdot','div','pm','mp',
            'leq','geq','neq','approx','equiv','propto',
            'rightarrow','leftarrow','Rightarrow','Leftarrow',
            'leftrightarrow','Leftrightarrow',
            'infty','partial','nabla','forall','exists',
            'alpha','beta','gamma','delta','epsilon','varepsilon',
            'zeta','eta','theta','vartheta','iota','kappa',
            'lambda','mu','nu','xi','pi','varpi','rho','varrho',
            'sigma','varsigma','tau','upsilon','phi','varphi',
            'chi','psi','omega',
            'Gamma','Delta','Theta','Lambda','Xi','Pi',
            'Sigma','Upsilon','Phi','Psi','Omega',
            'hbar','ell','Re','Im','wp','aleph',
            'ddots','vdots',
            'left','right','big','Big','bigg','Bigg',
            'Longrightarrow','longrightarrow','Longleftarrow','longleftrightarrow',
            'mapsto','longmapsto','hookrightarrow','hookleftarrow',
            'overrightarrow','overleftarrow','overbrace','underbrace',
            'vec','hat','bar','tilde','dot','ddot','check','acute','grave',
            'overline','underline','widehat','widetilde',
            'frac','binom','choose',
        }
        if name in _MATH_NO_ARGS:
            return f'\\{name} ', i

        # ── Environments triggered by \begin ──
        if name == 'begin':
            return self._begin(text, i)

        if name == 'end':
            _, i = get_arg(text, i)   # consume env name; we handle via find_env_end
            return '', i

        # ── Problem-set / assistance ──
        if name == 'BriefAns':
            return '<h4 class="brief-ans">Brief Answers</h4>\n', i

        if name == 'MeGivens':
            # \MeGivens{title}{reference material provided during the exam}
            (title, content), i = get_n_args(text, i, 2)
            return (f'<div class="me-givens">'
                    f'<h4 class="me-givens-title">Given: {self._process(title)}</h4>'
                    f'{self._process(content)}</div>\n'), i

        if name == 'AsItem':
            (num, ref, content), i = get_n_args(text, i, 3)
            inner = self._process(content)
            origin = self._pretty_asref(ref)
            origin_html = (f' <span class="help-origin">(from {origin})</span>'
                           if origin else '')
            return (f'<div id="help-{num}" class="help-item">'
                    f'<h3 class="help-num">S-{num}{origin_html}</h3>'
                    f'<div class="help-body">{inner}</div></div>\n'), i

        # ── Metadata macros (dat file) ──
        if name == 'IdVersEval':
            _, i = get_arg(text, i)   # date
            _, i = get_arg(text, i)   # version number
            return '', i

        if name in ('defModTitle', 'defCtAuthor', 'defIdAuthor', 'defIdItems',
                    'IdHours', 'SectType', 'revhist', 'defmodlength'):
            _, i = get_arg(text, i)
            return '', i

        if name == 'NsfAcknowledgment':
            return ('<p class="acknowledgment">This module was developed by '
                    'Project PHYSNET with support from the National Science '
                    'Foundation.</p>\n'), i

        # ── Spacing / layout ──
        if name in ('noindent', 'medskip', 'bigskip', 'smallskip',
                    'vspace', 'hspace', 'vskip', 'hskip',
                    'enlargethispage', 'enlargethispage*', 'addvspace',
                    'vfill', 'vfil', 'hfill', 'hfil', 'raggedright',
                    'raggedbottom', 'flushbottom'):
            # Consume optional argument if present
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return '', i

        if name == 'newline':
            return '<br>\n', i

        # Math spacing/symbol macros that leak into running text (between
        # adjacent \m{} groups, inside \Quote{}, etc.).  \m{}/$..$ math goes
        # through convert_math, which keeps these, so this only fires in text.
        if name in ('ldots', 'dots', 'textellipsis'):
            return '&hellip;', i
        if name == 'cdots':
            return '&middot;&middot;&middot;', i
        if name in ('quad',):
            return '&emsp;', i
        if name in ('qquad',):
            return '&emsp;&emsp;', i

        if name in ('par',):
            # Paragraph break — consume one braced group if present (Reif style \par{...})
            if i < len(text) and text[i] == '{':
                content, i = get_arg(text, i)
                return f'<p>{self._process(content)}', i
            return '<p>', i

        if name in ('nointerlineskip', 'allowbreak', 'obeylines',
                    'sloppy', 'fussy', 'emergencystretch'):
            return '', i

        if name in ('newpage', 'clearpage', 'pagebreak'):
            return '', i

        if name in ('label', 'ref', 'pageref'):
            _, i = get_arg(text, i)
            return '', i

        if name == 'newsavebox':
            _, i = get_arg(text, i)
            return '', i

        if name == 'sbox':
            _, i = get_arg(text, i)   # box name
            _, i = get_arg(text, i)   # content
            return '', i

        if name == 'usebox':
            _, i = get_arg(text, i)
            return '', i

        if name == 'inits':
            arg, i = get_arg(text, i)
            # arg may already end with a period (e.g. "M."), don't double it
            return arg if arg.endswith('.') else f'{arg}.', i

        if name == 'ph':
            # \ph{text} - phantom / invisible text; just render it
            arg, i = get_arg(text, i)
            return self._process(arg), i

        # ── Pass-through text commands ──
        if name in ('texttt', 'textsc', 'textrm', 'textsf'):
            arg, i = get_arg(text, i)
            return self._process(arg), i

        if name == 'text':
            # inside math – just pass through with \text{}
            arg, i = get_arg(text, i)
            return f'\\text{{{arg}}}', i

        # ── Font switches (no arg; used inside {groups}) ──
        if name in ('em', 'it', 'bf', 'rm', 'sf', 'sc', 'tt', 'cal', 'mit',
                    'normalfont', 'upshape', 'bfseries', 'itshape', 'slshape',
                    'sffamily', 'ttfamily', 'rmfamily', 'mdseries'):
            return '', i   # font style lost but text renders normally

        # ── Text formatting ──
        if name == 'Emph':
            arg, i = get_arg(text, i)
            return f'<em>{self._process(arg)}</em>', i

        if name == 'nth':
            arg, i = get_arg(text, i)
            return f'{self._process(arg)}<sup>th</sup>', i

        if name == 'AA':
            return 'Å', i

        if name == 'degreesC':
            return '°C', i

        if name == 'degreesF':
            return '°F', i

        if name == 'url':
            arg, i = get_arg(text, i)
            return f'<a href="{arg}" class="url">{arg}</a>', i

        if name == 'ccode':
            arg, i = get_arg(text, i)
            return f'<code>{html_lib.escape(self._process(arg))}</code>', i

        if name == 'fbox':
            arg, i = get_arg(text, i)
            return f'<span class="fbox">{self._process(arg)}</span>', i

        if name == 'parbox':
            # \parbox[align]{width}{content}
            j = i
            while j < len(text) and text[j] in ' \t\n':
                j += 1
            if j < len(text) and text[j] == '[':
                end = text.find(']', j)
                i = (end + 1) if end != -1 else j
            _, i = get_arg(text, i)          # width – discard
            content_arg, i = get_arg(text, i)
            return self._process(content_arg), i

        if name == 'raisebox':
            _, i = get_arg(text, i)          # height
            for _ in range(2):               # up to 2 optional [h][d] args
                j = i
                while j < len(text) and text[j] in ' \t\n':
                    j += 1
                if j < len(text) and text[j] == '[':
                    end = text.find(']', j)
                    i = (end + 1) if end != -1 else j
                else:
                    break
            content_arg, i = get_arg(text, i)
            return self._process(content_arg), i

        if name == 'BlackTriangle':
            return '▶ ', i

        if name in ('writein', 'OneInchAnswer', 'TwoInchAnswer',
                    'HalfInchAnswer', 'ThreeInchAnswer'):
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return '<span class="writein">___________</span>', i

        # ── Additional equation / figure references ──
        if name == 'Eqnssref':
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eq.&nbsp;({num})</a>', i

        if name in ('Equationref', 'Equationsref'):
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eq.&nbsp;({num})</a>', i

        if name in ('Eqnstoref', 'Eqnsstoref', 'Equationstoref'):
            n1, i = get_arg(text, i)
            n2, i = get_arg(text, i)
            return (f'<a href="#eqn-{n1}" class="eqn-ref">'
                    f'Eqs.&nbsp;({n1})–({n2})</a>'), i

        if name == 'Ineqref':
            num, i = get_arg(text, i)
            return f'<a href="#ineq-{num}" class="eqn-ref">Ineq.&nbsp;({num})</a>', i

        if name in ('Figstoref', 'Figsstoref'):
            n1, i = get_arg(text, i)
            n2, i = get_arg(text, i)
            return (f'<a href="#fig-{n1}" class="fig-ref">'
                    f'Figs.&nbsp;{n1}–{n2}</a>'), i

        # ── Layout / spacing (additional) ──
        if name in ('hfil', 'hfill', 'strut', 'centering', 'raggedright',
                    'raggedleft', 'nobreakspace', 'linebreak', 'allowbreak'):
            return '', i

        if name in ('baselineskip', 'parindent', 'tabcolsep', 'arraycolsep',
                    'parskip', 'lineskip', 'itemsep', 'parsep', 'partopsep',
                    'topsep', 'leftmargin', 'rightmargin', 'arraystretch'):
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return '', i

        if name in ('vspace*', 'hspace*'):
            _, i = get_arg(text, i)
            return '', i

        if name == 'setlength':
            _, i = get_arg(text, i)
            _, i = get_arg(text, i)
            return '', i

        if name == 'addtolength':
            _, i = get_arg(text, i)
            _, i = get_arg(text, i)
            return '', i

        if name == 'phantom':
            _, i = get_arg(text, i)
            return '', i

        if name in ('cline', 'vline'):
            _, i = get_arg(text, i)
            return '', i

        if name == 'hline':
            return '', i

        if name == 'multicolumn':
            _, i = get_arg(text, i)   # cols
            _, i = get_arg(text, i)   # alignment
            content_arg, i = get_arg(text, i)
            return self._process(content_arg), i

        # ── LeftEqn – left-aligned display equation ──
        if name == 'LeftEqn':
            _, i = get_arg(text, i)   # label (usually empty)
            eq, i = get_arg(text, i)
            return f'\\[{convert_math(eq)}\\]\n', i

        # ── FourEqns – 4-line aligned block ──
        if name == 'FourEqns':
            _, i = get_arg(text, i)
            eqs = []
            for _ in range(4):
                eq, i = get_arg(text, i)
                eqs.append(convert_math(eq))
            joined = ' \\\\ '.join(eqs)
            return f'\\[\\begin{{aligned}}{joined}\\end{{aligned}}\\]\n', i

        # ── TwoColsTwoEqns – two equations with side annotations ──
        if name == 'TwoColsTwoEqns':
            _, i = get_arg(text, i)
            eq1,  i = get_arg(text, i)
            txt1, i = get_arg(text, i)
            eq2,  i = get_arg(text, i)
            txt2, i = get_arg(text, i)
            t1 = self._process(txt1)
            t2 = self._process(txt2)
            return (f'\\[\\begin{{aligned}}'
                    f'{convert_math(eq1)} && \\quad {t1} \\\\ '
                    f'{convert_math(eq2)} && \\quad {t2}'
                    f'\\end{{aligned}}\\]\n'), i

        # ── TextAndFigure – text paragraph alongside figure ──
        if name == 'TextAndFigure':
            text_content, i = get_arg(text, i)
            return self._process(text_content), i

        # ── Additional figure macros ──
        if name in ('CaptionedFullUnframedFigure', 'CaptionedFullFramedFixedFigure'):
            return self._figures(text, i, 1, full=True)

        if name == 'CaptionedLeftUnframedFigure':
            num,   i = get_arg(text, i)
            cap,   i = get_arg(text, i)
            fname, i = get_arg(text, i)
            cap_html = self._process(cap)
            return (f'<figure id="fig-{num}" class="fig-left">'
                    f'<img src="{self.figures_path}/{fname}.svg" '
                    f'alt="Figure {num}" class="physnet-fig">'
                    f'<figcaption>Fig.&nbsp;{num}. {cap_html}</figcaption>'
                    f'</figure>\n'), i

        # ── Boilerplate / acknowledgments ──
        if name == 'Acknowledgments':
            return '<h4>Acknowledgments</h4>\n', i

        if name == 'IsuAcknowledgment':
            return ('<p class="acknowledgment">This module was developed at '
                    'Iowa State University.</p>\n'), i

        if name in ('BriefAnsNewPage', 'ReadingsAccess', 'SeeLocalGuide',
                    'LineFill', 'NullItem'):
            return '', i

        if name == 'GlossaryItem':
            _, i = get_arg(text, i)   # keyword
            _, i = get_arg(text, i)   # definition
            _, i = get_arg(text, i)   # module ref
            return '', i

        if name == 'answer':
            _, i = get_arg(text, i)
            return '', i

        if name == 'item':
            # bare \item outside a list env — consume optional label
            j = i
            while j < len(text) and text[j] in ' \t\n':
                j += 1
            if j < len(text) and text[j] == '[':
                end = text.find(']', j)
                i = (end + 1) if end != -1 else j
            return '<br>\n', i

        # ── Meta / preamble commands ──
        if name in ('renewcommand', 'newcommand', 'providecommand'):
            _, i = get_arg(text, i)   # command name
            for _ in range(2):        # optional [nargs][default]
                j = i
                while j < len(text) and text[j] in ' \t\n':
                    j += 1
                if j < len(text) and text[j] == '[':
                    end = text.find(']', j)
                    i = (end + 1) if end != -1 else j
                else:
                    break
            _, i = get_arg(text, i)   # definition
            return '', i

        if name == 'input':
            _, i = get_arg(text, i)
            return '', i

        if name in ('TxEnd', 'PsEnd', 'AsEnd', 'MeEnd', 'IdEnd'):
            return '', i

        if name in ('lhead', 'chead', 'rhead', 'lfoot', 'cfoot', 'rfoot'):
            _, i = get_arg(text, i)
            return '', i

        if name in ('ComputerProjectExam', 'ComputerProjectGrader',
                    'ComputerProjectPoints', 'IsuAcknowledgment'):
            return '', i

        # ── vectprime ──
        if name == 'vectprime':
            arg, i = get_arg(text, i)
            return f"\\(\\vec{{{arg}}}'\\)", i

        # ── dn (subscript in particle physics tables) ──
        if name == 'dn':
            arg, i = get_arg(text, i)
            return f'<sub>{self._process(arg)}</sub>', i

        # ── Particle physics symbols ──
        _QUARKS = {
            'qu': 'u', 'qd': 'd', 'qs': 's', 'qc': 'c', 'qt': 't', 'qb': 'b',
            'qub': 'ū', 'qdb': 'd̄', 'qsb': 's̄', 'qcb': 'c̄', 'qtb': 't̄',
            'qqb': 'q̄', 'qq': 'qq',
        }
        if name in _QUARKS:
            return f'<em class="quark">{_QUARKS[name]}</em>', i

        _MESONS = {
            'mpi': 'π', 'mK': 'K', 'mKb': 'K̄', 'meta': 'η', 'metap': 'η′',
            'mrho': 'ρ', 'momega': 'ω', 'mphi': 'φ', 'mW': 'W', 'mgamma': 'γ',
            'mAtwo': 'A₂', 'mf': 'f', 'mfp': 'f′',
        }
        if name in _MESONS:
            return f'<em class="meson">{_MESONS[name]}</em>', i

        _BARYONS = {
            'bLambda': 'Λ', 'bSigma': 'Σ', 'bOmega': 'Ω', 'bDelta': 'Δ',
            'bXi': 'Ξ', 'bXib': 'Ξ̄', 'bn': 'n', 'bp': 'p',
        }
        if name in _BARYONS:
            return f'<em class="baryon">{_BARYONS[name]}</em>', i

        _LEPTONS = {
            'lmu': 'μ', 'lnue': 'ν<sub>e</sub>',
            'lnueb': 'ν̄<sub>e</sub>', 'lnumu': 'ν<sub>μ</sub>',
            'lnumub': 'ν̄<sub>μ</sub>', 'lnutau': 'ν<sub>τ</sub>', 'ltau': 'τ',
        }
        if name in _LEPTONS:
            return f'<em class="lepton">{_LEPTONS[name]}</em>', i

        # ── dfrac / sqrt in text context (typically inside LeftEqn or similar) ──
        if name == 'dfrac':
            n_arg, i = get_arg(text, i)
            d_arg, i = get_arg(text, i)
            return (f'\\(\\dfrac{{{convert_math(n_arg)}}}'
                    f'{{{convert_math(d_arg)}}}\\)'), i

        if name == 'sqrt':
            arg, i = get_arg(text, i)
            return f'\\(\\sqrt{{{convert_math(arg)}}}\\)', i

        # ── Unknown: emit a visible marker for debugging ──
        return f'<span class="tex-unknown" title="unknown macro">\\{name}</span>', i

    # ── Section handler ───────────────────────────────────────────────────────

    def _sect(self, text, i):
        # Skip optional [index entries] argument if present
        i = skip_opt_arg(text, i)
        # \Sect comes in a 4-arg form  {num}{title}{\SectType{..} or {}}{body}
        # and an older 3-arg form      {num}{title}{body}.  Grab three, then
        # decide whether the third is a section-type marker or the body.
        (num, title, third), i = get_n_args(text, i, 3)
        if third.strip() == '' or '\\SectType' in third:
            sect_type_raw = third
            content, i = get_arg(text, i)
        else:
            sect_type_raw = ''
            content = third
        inner      = self._process(content)
        title_html = self._process(title)

        # Determine section type from \SectType{...}
        m = re.search(r'\\SectType\s*\{(\w+)\}', sect_type_raw)
        sect_type = m.group(1) if m else ''

        if sect_type == 'ProblemSet':
            heading = 'Problem Supplement'
            css_cls = 'section-problems'
        elif sect_type == 'SpecialAssistance':
            heading = 'Special Assistance Supplement'
            css_cls = 'section-assistance'
        elif sect_type == 'ModelExam':
            heading = 'Model Exam'
            css_cls = 'section-exam'
        elif sect_type == 'Acknowledgments':
            heading = 'Acknowledgments'
            css_cls = 'section-ack'
        else:
            heading = title_html
            css_cls = 'section-text'

        if num:
            sec_id = f'sect-{num}'
            h_tag  = f'<h2 id="{sec_id}">{num}. {title_html}</h2>'
        elif heading:
            sec_id = re.sub(r'\W+', '-', heading.lower()).strip('-')
            h_tag  = f'<h2 id="{sec_id}">{heading}</h2>'
        else:
            sec_id = ''
            h_tag  = ''

        return (f'<section id="{sec_id}" class="module-section {css_cls}">'
                f'{h_tag}\n{inner}</section>\n'), i

    def _pretty_asref(self, ref):
        """The 2nd \\AsItem arg records where the hint is used, e.g.
        'TX-4a', 'PS-19c', 'PS-Problem~1', 'TX-2a, TX-2f, [S-6]'.
        Render it the way the printed module does: 'Section 4a', etc."""
        ref = ref.replace('~', ' ').replace('\\,', ' ').strip()
        if not ref:
            return ''
        out = []
        for part in (p.strip() for p in ref.split(',')):
            if not part:
                continue
            m = re.match(r'TX-(.+)$', part)
            if m:
                out.append(f'Section&nbsp;{m.group(1)}')
                continue
            m = re.match(r'PS-Problem\s+(.+)$', part)
            if m:
                out.append(f'Problem&nbsp;Supplement, Problem&nbsp;{m.group(1)}')
                continue
            m = re.match(r'PS-(.+)$', part)
            if m:
                out.append(f'Problem&nbsp;Supplement&nbsp;{m.group(1)}')
                continue
            out.append(part)
        return ', '.join(out)

    # ── Figure handler ────────────────────────────────────────────────────────

    def _reif_fig(self, fig_id, cap_html, fname, extra_cls=''):
        """<figure> for a Reif-format figure macro (\\FullFigure etc.).
        Figures live in public/modules/<id>/figures/, referenced relative to
        the module page as figures/<name>.svg."""
        fname = re.sub(r'\.(eps|ps|pdf|png|jpe?g)$', '', fname.strip())
        cap = cap_html.strip()
        cap_html = (f'<figcaption><strong>Fig.&nbsp;{fig_id}.</strong>'
                    + (f' {cap}' if cap else '') + '</figcaption>') if fig_id or cap else ''
        if not fname:
            # disabled/commented-out figure in the source -- keep the caption
            # if there is one, drop the broken <img>.
            return (f'<figure id="fig-{fig_id}" class="module-figure">{cap_html}'
                    f'</figure>\n') if cap else ''
        return (f'<figure id="fig-{fig_id}" class="module-figure{extra_cls}">'
                f'<img src="figures/{fname}.svg" alt="Figure {fig_id}" '
                f'class="physnet-fig">{cap_html}</figure>\n')

    def _figures(self, text, i, count, full=False):
        figs = []
        for _ in range(count):
            num,   i = get_arg(text, i)
            cap,   i = get_arg(text, i)
            fname, i = get_arg(text, i)
            cap_html = self._process(cap) if cap else ''
            figs.append((num, cap_html, fname))

        css_cls = 'figure-full' if full else f'figure-row figure-row-{count}'
        parts   = []
        for num, cap_html, fname in figs:
            cap_html = cap_html.strip()
            caption = f'Fig.&nbsp;{num}.' + (f' {cap_html}' if cap_html else '')
            parts.append(
                f'<figure id="fig-{num}">'
                f'<img src="{self.figures_path}/{fname}.svg" '
                f'alt="Figure {num}" class="physnet-fig">'
                f'<figcaption>{caption}</figcaption>'
                f'</figure>'
            )
        return f'<div class="{css_cls}">{"".join(parts)}</div>\n', i

    # ── Environment handler ───────────────────────────────────────────────────

    def _begin(self, text, i):
        env, i = get_arg(text, i)
        content, i = find_env_end(text, i, env)

        if env in ('itemize', 'SummarySubItems', 'zero-digit-list'):
            return self._list(content, 'ul'), i

        if env in ('enumerate', 'one-digit-list', 'two-digit-list'):
            return self._list(content, 'ol'), i

        if env == 'SummaryItems':
            return (f'<div class="summary-items">{self._process(content)}</div>\n'), i

        if env in ('eqnarray', 'eqnarray*'):
            return self._eqnarray(content), i

        if env in ('equation', 'equation*', 'displaymath'):
            eq = convert_math(content.strip())
            return f'\\[{eq}\\]\n', i

        if env in ('align', 'align*', 'aligned'):
            eq = convert_math(content.strip())
            return f'\\[\\begin{{aligned}}{eq}\\end{{aligned}}\\]\n', i

        if env == 'InputSkills':
            return self._skill_env(content, 'Input Skills'), i

        if env == 'KnowledgeSkills':
            return self._skill_env(content, 'Output Skills (Knowledge)'), i

        if env == 'RuleApplicationSkills':
            return self._skill_env(content, 'Output Skills (Rule Application)'), i

        if env in ('ProblemApplicationSkills', 'ProblemSolvingSkills'):
            return self._skill_env(content, 'Output Skills (Problem Solving)'), i

        if env == 'PostOptions':
            return self._skill_env(content, 'Post-Options'), i

        if env in ('tabular', 'tabular*'):
            return self._tabular(content), i

        if env in ('table', 'table*'):
            inner = self._process(content)
            return f'<div class="table-float">{inner}</div>\n', i

        if env == 'RequiredResources':
            inner = self._list(content, 'ol')
            return (f'<div class="required-resources">'
                    f'<h4>Required Resources</h4>{inner}</div>\n'), i

        if env in ('verbatim', 'verbatim*'):
            return f'<pre>{html_lib.escape(content)}</pre>', i

        if env in ('center',):
            inner = self._process(content)
            return f'<div class="centered">{inner}</div>\n', i

        # Generic: process content
        inner = self._process(content)
        return f'<div class="env-{env}">{inner}</div>\n', i

    def _list(self, content, tag):
        """Convert a LaTeX list environment to HTML <ul>/<ol>."""
        # Split on \item, keeping optional label
        items_raw = re.split(r'\\item\b', content)
        html_items = []
        for raw in items_raw[1:]:   # first split before first \item is preamble
            # Optional label: [a.] or [K1.] etc.
            m = re.match(r'\s*\[([^\]]*)\]\s*', raw)
            if m:
                label = m.group(1).strip()
                body  = raw[m.end():]
            else:
                label = ''
                body  = raw
            inner = self._process(body.strip())
            if label:
                html_items.append(
                    f'<li><span class="item-label">{label}</span> {inner}</li>')
            else:
                html_items.append(f'<li>{inner}</li>')
        return f'<{tag}>\n' + '\n'.join(html_items) + f'\n</{tag}>\n'

    def _eqnarray(self, content):
        """Convert eqnarray* to an aligned LaTeX block for KaTeX.

        eqnarray rows are 3-column: ``lhs & rel & rhs``.  KaTeX's ``aligned``
        is 2-column (``lhs & rhs``), so fold the middle relation column into
        the right-hand side: ``A & = & B`` -> ``A &= B``.
        """
        rows = re.split(r'\\\\', content.strip())
        fixed = []
        for row in rows:
            if not row.strip():
                continue
            cells = row.split('&')
            if len(cells) >= 3:
                row = (cells[0].rstrip() + ' &' + cells[1].strip() + ' '
                       + '&'.join(cells[2:]).lstrip())
            fixed.append(row.strip())
        eq = convert_math(' \\\\ '.join(fixed))
        return f'\\[\\begin{{aligned}}{eq}\\end{{aligned}}\\]\n'

    def _skill_env(self, content, heading):
        items_raw = re.split(r'\\item\b', content)
        html_items = []
        for raw in items_raw[1:]:
            m = re.match(r'\s*\[([^\]]*)\]\s*', raw)
            label = m.group(1) if m else ''
            body  = raw[m.end():] if m else raw
            inner = self._process(body.strip())
            html_items.append(
                f'<li><strong>{label}</strong> {inner}</li>' if label
                else f'<li>{inner}</li>')
        return (f'<div class="skills-block">'
                f'<h4>{heading}</h4>'
                f'<ul>{"".join(html_items)}</ul></div>\n')

    # ── Table handler ────────────────────────────────────────────────────────

    def _tabular(self, content):
        """Convert tabular environment to a basic HTML table."""
        # First arg is the column spec {lcc|r} — consume and discard
        col_spec, pos = get_arg(content, 0)
        content = content[pos:]
        # A leading \hline before the first row marks it as a header row.
        header_first = content.lstrip().startswith('\\hline')
        # Split rows on \\
        rows = re.split(r'\\\\', content)
        html_rows = []
        seen_row = False
        for row in rows:
            row = re.sub(r'\\hline|\\cline\{[^}]*\}', '', row).strip()
            if not row:
                continue
            cells = row.split('&')
            if not any(c.strip() for c in cells):
                continue
            tag = 'th' if (header_first and not seen_row) else 'td'
            seen_row = True
            html_cells = ''.join(
                f'<{tag}>{self._process(c.strip())}</{tag}>' for c in cells)
            html_rows.append(f'<tr>{html_cells}</tr>')
        if not html_rows:
            return ''
        return (f'<table class="physnet-table">'
                f'<tbody>{"".join(html_rows)}</tbody></table>\n')

    # ── Footnote rendering ────────────────────────────────────────────────────

    def render_footnotes(self):
        if not self.footnotes:
            return ''
        items = []
        for num, html in self.footnotes:
            items.append(
                f'<li id="fn-{num}">{html} '
                f'<a href="#fnref-{num}" class="fn-back">↩</a></li>')
        return ('<aside class="footnotes"><h4>Notes</h4>'
                f'<ol>{"".join(items)}</ol></aside>\n')


# ── Module metadata parser ────────────────────────────────────────────────────

def parse_dat(text, conv):
    """Extract metadata from mN-dat.tex. Returns dict."""
    text = conv._strip_comments(text)
    text = conv._strip_preamble(text)

    def extract(macro, nargs=1):
        pat = re.compile(r'\\' + re.escape(macro) + r'\s*\{')
        m = pat.search(text)
        if not m:
            return [''] * nargs
        pos = m.end() - 1   # back to '{'
        args, _ = get_n_args(text, pos, nargs)
        return args

    title  = conv._process(extract('defModTitle')[0])
    author = conv._process(extract('defCtAuthor')[0])

    # Skills / objectives come from \defIdItems{...}
    id_items_raw = extract('defIdItems')[0]
    id_items_html = conv._process(id_items_raw) if id_items_raw else ''

    # Study time
    m = re.search(r'\\IdHours\s*\{([^}]+)\}', text)
    hours = m.group(1) if m else ''

    return {
        'title':        title,
        'author':       author,
        'id_items':     id_items_html,
        'hours':        hours,
    }


# ── Full module assembler ─────────────────────────────────────────────────────

# Modules visually confirmed to open with a decorative cover cartoon
# (mNgr00, placed by nphmods.sty, not referenced in the text).
COVER_MODULES = {'m1', 'm4', 'm10'}


def convert_module(module_dir, module_id):
    def read(suffix):
        path = os.path.join(module_dir, f'{module_id}{suffix}')
        if os.path.exists(path):
            with open(path, encoding='utf-8', errors='replace') as f:
                return f.read()
        return ''

    conv = PhysnetConverter(module_id, figures_path='figures')

    # Parse metadata
    dat_text = read('-dat.tex')
    meta = parse_dat(dat_text, conv) if dat_text else {}

    # Convert each section (try -tx.tex first, then -b.tex as fallback)
    tx_text = read('-tx.tex') or read('-b.tex')
    tx_html  = conv.convert(tx_text)
    ps_html  = conv.convert(read('-ps.tex'))
    as_html  = conv.convert(read('-as.tex'))
    me_html  = conv.convert(read('-me.tex'))

    footnotes_html = conv.render_footnotes()

    # Assemble
    parts = []

    # ── Header ──
    parts.append(f'<header class="module-header">')
    title = meta.get('title', '')
    author = meta.get('author', '')
    if title and author:
        parts.append(f'  <h1 class="module-title">{title} <span class="module-author">({author})</span></h1>')
    elif title:
        parts.append(f'  <h1 class="module-title">{title}</h1>')
    parts.append(f'  <div class="module-meta-row">')
    if meta.get('hours'):
        parts.append(f'    <span class="meta-pill">⏱ {meta["hours"]} hour(s)</span>')
    parts.append(f'  </div>')
    parts.append('</header>\n')

    # ── Cover graphic ──
    # A few MISN modules open with a decorative cover cartoon (mNgr00),
    # placed by the nphmods.sty template and never referenced by a figure
    # macro.  But mNgr00 is just as often "Figure 1", a leftover copy of
    # another module's cover, or orphaned draft content -- indistinguishable
    # without looking at the picture.  So inject it only for modules that
    # have been visually confirmed to have one.
    if (module_id in COVER_MODULES
            and f'{module_id}gr00.svg' not in tx_html
            and (os.path.exists(os.path.join(module_dir, 'figures',
                                             f'{module_id}gr00.svg'))
                 or os.path.exists(os.path.join('public', 'modules', module_id,
                                                'figures', f'{module_id}gr00.svg')))):
        parts.append('<figure class="fig-centered module-cover-fig">')
        parts.append(f'<img src="figures/{module_id}gr00.svg" alt="" '
                     f'class="physnet-fig">')
        parts.append('</figure>\n')

    # ── Learning objectives ──
    if meta.get('id_items'):
        parts.append('<section id="objectives" class="module-section section-objectives">')
        parts.append('<h2>Learning Objectives</h2>')
        parts.append(meta['id_items'])
        parts.append('</section>\n')

    # ── Main text ──
    if tx_html.strip():
        parts.append(tx_html)

    # ── Footnotes ──
    if footnotes_html:
        parts.append(footnotes_html)

    # ── Problem set ──
    if ps_html.strip():
        parts.append('<hr class="section-divider">\n')
        parts.append(ps_html)

    # ── Special assistance ──
    if as_html.strip():
        parts.append('<hr class="section-divider">\n')
        parts.append(as_html)

    # ── Model exam ──
    if me_html.strip():
        parts.append('<hr class="section-divider">\n')
        parts.append(me_html)

    html = '\n'.join(parts)

    # ── Localise cross-module figure references ──
    # A module that reuses another module's figure (e.g. m3-tx.tex writes
    # \CaptionedFullFramedFigure{1}{...}{m1gr01}) ships its own copy of that
    # EPS in its source folder (m3gr01.eps).  Rewrite the reference to this
    # module's prefix whenever the local copy exists, so each module's page
    # pulls only from its own public/modules/<id>/figures/ directory.
    def _localise(m):
        num = m.group(2)
        local_svg = os.path.join('public', 'modules', module_id, 'figures',
                                 f'{module_id}gr{num}.svg')
        local_eps = os.path.join(module_dir, f'{module_id}gr{num}.eps')
        if m.group(1) != module_id and (os.path.exists(local_svg) or
                                        os.path.exists(local_eps)):
            return f'src="figures/{module_id}gr{num}.svg"'
        return m.group(0)
    html = re.sub(r'src="figures/([A-Za-z0-9]+)gr(\d+)\.svg"', _localise, html)

    return html


# ── CLI entry point ───────────────────────────────────────────────────────────

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        sys.exit(1)

    module_dir = sys.argv[1]
    module_id  = sys.argv[2]

    result = convert_module(module_dir, module_id)
    sys.stdout.write(result)
