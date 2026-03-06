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
    s = re.sub(r'\\vect\{([^{}]*)\}', r'\\vec{\1}', s)
    s = re.sub(r'\\uvec\{([^{}]*)\}', r'\\hat{\1}', s)
    s = re.sub(r'\\up\{([^{}]*)\}',   r'^{\1}',     s)
    s = re.sub(r'\\unit\{([^{}]*)\}', r'\\,\\text{\1}', s)
    s = s.replace(r'\degrees', r'^\circ')
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
        text = self._strip_comments(text)
        text = self._strip_preamble(text)
        return self._process(text)

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

        # ── Inline math ──
        if name == 'm':
            arg, i = get_arg(text, i)
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
            _label, i = get_arg(text, i)
            eq, i     = get_arg(text, i)
            return f'\\[{convert_math(eq)}\\]', i

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
            return f'<a href="#fig-{num}" class="fig-ref">Figs.&nbsp;{num}</a>', i

        if name == 'Eqnref':
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eq.&nbsp;({num})</a>', i

        if name == 'Eqnsref':
            num, i = get_arg(text, i)
            return f'<a href="#eqn-{num}" class="eqn-ref">Eqs.&nbsp;({num})</a>', i

        if name == 'AsSect':
            num, i = get_arg(text, i)
            return f'<a href="#as-{num}" class="as-ref">AS&nbsp;{num}</a>', i

        if name == 'help':
            num, i = get_arg(text, i)
            return (f'<a href="#help-{num}" class="help-link">'
                    f'<span title="Special Assistance">[help&nbsp;{num}]</span></a>'), i

        if name in ('prrqone', 'prrqtwo'):
            ref, i = get_arg(text, i)
            # ref like "0-14" → module number is the last segment
            mid = ref.split('-')[-1]
            return (f'<a href="../m{mid}/" class="module-ref">'
                    f'MISN-0-{mid}</a>'), i

        # ── Section structure ──
        if name == 'Sect':
            return self._sect(text, i)

        if name in ('pcap', 'xpcap'):
            (sec, letter, title), i = get_n_args(text, i, 3)
            title_html = self._process(title)
            pid = f'pcap-{sec}-{letter}'
            return f'<h3 id="{pid}">{sec}{letter}. {title_html}</h3>\n', i

        if name == 'icap':
            title, i = get_arg(text, i)
            return f'<p class="icap"><strong>{self._process(title)}:</strong></p>\n', i

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
            'ldots','cdots','ddots','vdots',
            'left','right','big','Big','bigg','Bigg',
            'quad','qquad',
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

        if name == 'AsItem':
            (num, ref, content), i = get_n_args(text, i, 3)
            inner = self._process(content)
            return (f'<div id="help-{num}" class="help-item">'
                    f'<span class="help-num">{num}.</span>'
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
                    'vspace', 'hspace', 'vskip', 'hskip'):
            # Consume optional argument if present
            if i < len(text) and text[i] == '{':
                _, i = get_arg(text, i)
            return '', i

        if name == 'newline':
            return '<br>\n', i

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
            return f'{arg}.', i

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

        # ── Unknown: emit a visible marker for debugging ──
        return f'<span class="tex-unknown" title="unknown macro">\\{name}</span>', i

    # ── Section handler ───────────────────────────────────────────────────────

    def _sect(self, text, i):
        # Skip optional [index entries] argument if present
        n = len(text)
        j = i
        while j < n and text[j] in ' \t\n':
            j += 1
        if j < n and text[j] == '[':
            depth = 0
            while j < n:
                if text[j] == '[': depth += 1
                elif text[j] == ']':
                    depth -= 1
                    if depth == 0:
                        j += 1
                        break
                j += 1
            i = j
        (num, title, sect_type_raw, content), i = get_n_args(text, i, 4)
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

    # ── Figure handler ────────────────────────────────────────────────────────

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
            parts.append(
                f'<figure id="fig-{num}">'
                f'<img src="{self.figures_path}/{fname}.svg" '
                f'alt="Figure {num}" class="physnet-fig">'
                f'<figcaption>Fig.&nbsp;{num}. {cap_html}</figcaption>'
                f'</figure>'
            )
        return f'<div class="{css_cls}">{"".join(parts)}</div>\n', i

    # ── Environment handler ───────────────────────────────────────────────────

    def _begin(self, text, i):
        env, i = get_arg(text, i)
        content, i = find_env_end(text, i, env)

        if env in ('itemize',):
            return self._list(content, 'ul'), i

        if env in ('enumerate', 'one-digit-list', 'two-digit-list'):
            return self._list(content, 'ol'), i

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

        if env in ('tabular', 'table', 'table*'):
            return f'<!-- table omitted -->', i

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
        """Convert eqnarray* to an aligned LaTeX block for KaTeX."""
        # eqnarray uses & for alignment, \\ for new row
        # KaTeX's aligned environment uses & and \\
        eq = convert_math(content.strip())
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

    # Convert each section
    tx_html  = conv.convert(read('-tx.tex'))
    ps_html  = conv.convert(read('-ps.tex'))
    as_html  = conv.convert(read('-as.tex'))
    me_html  = conv.convert(read('-me.tex'))

    footnotes_html = conv.render_footnotes()

    # Assemble
    parts = []

    # ── Header ──
    parts.append(f'<header class="module-header">')
    parts.append(f'  <div class="module-id-badge">{module_id.upper()}</div>')
    if meta.get('title'):
        parts.append(f'  <h1 class="module-title">{meta["title"]}</h1>')
    if meta.get('author'):
        parts.append(f'  <p class="module-author">{meta["author"]}</p>')
    parts.append(f'  <div class="module-meta-row">')
    if meta.get('hours'):
        parts.append(f'    <span class="meta-pill">⏱ {meta["hours"]} hour(s)</span>')
    parts.append(f'  </div>')
    parts.append('</header>\n')

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

    return '\n'.join(parts)


# ── CLI entry point ───────────────────────────────────────────────────────────

if __name__ == '__main__':
    if len(sys.argv) < 3:
        print(__doc__, file=sys.stderr)
        sys.exit(1)

    module_dir = sys.argv[1]
    module_id  = sys.argv[2]

    result = convert_module(module_dir, module_id)
    sys.stdout.write(result)
