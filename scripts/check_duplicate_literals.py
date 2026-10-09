#!/usr/bin/env python3
"""
Duplicate-text guardrail for the Kotlin sources (app, core-api, core-mock — main, not tests).
Called through scripts/check-duplicate-literals.sh; see that file for where it's wired.

Two rules, both about string literals only (comments and code are never read as text):

1. One spelling. A `const val NAME = "value"` declared directly under a `// ONE_SPELLING`
   comment owns that text: the value may not appear inside any other string literal in scope.
   This is how "ZenMode OS" stays in one place (AppConstants.PRODUCT_NAME) — a screen writes
   "Welcome to $PRODUCT_NAME", never the name itself. `grep -rn "ONE_SPELLING"` lists them.

2. No third copy. A literal long enough to be a sentence, a key or an identifier
   (MIN_LENGTH chars, MIN_LETTERS letters once templates are stripped) may appear at most
   MAX_COPIES times across the tree. The third copy fails: name it once (a const val, or a
   function that owns the behaviour the text belongs to) and use that. Annotation arguments
   are exempt. Literals already repeated when this check landed are grandfathered in
   scripts/duplicate-literal-debt.txt at their count — a ratchet: the count may fall, not grow.

Usage: check_duplicate_literals.py REPO_ROOT [file...]
  With files, only findings that touch those files are reported (the index is still the
  whole tree — a copy is only a copy relative to the others).
Pure standard library; Python 3.8+.
"""
import os
import re
import sys

MIN_LENGTH = 12
MIN_LETTERS = 6
MAX_COPIES = 2
SCOPE = ("app/src/main", "core-api/src/main", "core-mock/src/main")
DEBT_FILE = "scripts/duplicate-literal-debt.txt"
MARKER = "ONE_SPELLING"

ANNOTATION_ARG = re.compile(r"@[\w.]+\s*\([^()]*$")
TEMPLATE = re.compile(r"\$\{[^}]*\}|\$[A-Za-z_]\w*")


class Literal:
    __slots__ = ("text", "line", "prefix")

    def __init__(self, text, line, prefix):
        self.text = text        # the literal's source between its quotes, templates verbatim
        self.line = line        # 1-based line the literal opens on
        self.prefix = prefix    # code on that line before the opening quote


def literals(src):
    """Every string literal in Kotlin source [src], including ones nested in ${templates}."""
    found = []
    n = len(src)

    def line_at(i):
        return src.count("\n", 0, i) + 1

    def prefix_at(i):
        return src[src.rfind("\n", 0, i) + 1:i]

    def code(i, in_template):
        """Scan code from i; inside a ${template}, stop after its closing brace."""
        depth = 0
        while i < n:
            c = src[i]
            if src.startswith("//", i):
                i = src.find("\n", i)
                if i < 0:
                    return n
            elif src.startswith("/*", i):
                nest, i = 1, i + 2
                while i < n and nest:
                    if src.startswith("/*", i):
                        nest, i = nest + 1, i + 2
                    elif src.startswith("*/", i):
                        nest, i = nest - 1, i + 2
                    else:
                        i += 1
                continue
            elif c == "'":
                end = i + 1
                while end < n and src[end] != "'" and src[end] != "\n":
                    end += 2 if src[end] == "\\" else 1
                i = end + 1
                continue
            elif c == '"':
                i = string(i)
                continue
            elif c == "{":
                depth += 1
            elif c == "}":
                if depth == 0 and in_template:
                    return i + 1
                depth -= 1
            i += 1
        return n

    def string(start):
        raw = src.startswith('"""', start)
        i = start + (3 if raw else 1)
        body_start = i
        while i < n:
            if raw and src.startswith('"""', i):
                while src.startswith('""""', i):  # a raw string may end in quotes of its own
                    i += 1
                found.append(Literal(src[body_start:i], line_at(start), prefix_at(start)))
                return i + 3
            c = src[i]
            if not raw and c == "\\":
                i += 2
            elif not raw and c in '"\n':
                found.append(Literal(src[body_start:i], line_at(start), prefix_at(start)))
                return i + 1
            elif src.startswith("${", i):
                i = code(i + 2, True)
            else:
                i += 1
        return n

    code(0, False)
    return found


def kotlin_files(root):
    for top in SCOPE:
        for folder, _, names in os.walk(os.path.join(root, top)):
            for name in names:
                if name.endswith(".kt"):
                    yield os.path.join(folder, name)


def rel(root, path):
    return os.path.relpath(path, root).replace(os.sep, "/")


def letters(text):
    return sum(ch.isalpha() for ch in TEMPLATE.sub("", text))


def one_spellings(root, sources):
    """{value: (const name, file)} for every const declared under a ONE_SPELLING marker."""
    owned = {}
    for path, src in sources.items():
        lines = src.split("\n")
        for i, line in enumerate(lines[:-1]):
            if line.strip().startswith("//") and MARKER in line:
                decl = re.match(r'\s*(?:(?:private|internal|public)\s+)?const\s+val\s+(\w+)\s*(?::\s*String\s*)?=\s*"([^"$\\]+)"\s*$',
                                lines[i + 1])
                if not decl:
                    sys.exit(f"error: {rel(root, path)}:{i + 1}: // {MARKER} must sit directly above a "
                             f'`const val NAME = "plain text"` line.')
                owned[decl.group(2)] = (decl.group(1), path)
    return owned


def load_debt(root):
    debt = {}
    path = os.path.join(root, DEBT_FILE)
    if not os.path.exists(path):
        return debt
    with open(path, encoding="utf-8") as f:
        for number, line in enumerate(f, 1):
            line = line.rstrip("\n")
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            m = re.match(r'^(\d+)\s+"(.*)"$', line)
            if not m:
                sys.exit(f'error: {DEBT_FILE}:{number}: expected <count> "<literal>", got: {line}')
            debt[m.group(2)] = int(m.group(1))
    return debt


def main(argv):
    if len(argv) < 2:
        sys.exit("usage: check_duplicate_literals.py REPO_ROOT [file...]")
    root = os.path.abspath(argv[1])
    only = {os.path.abspath(p) for p in argv[2:]}
    sources = {}
    for path in kotlin_files(root):
        with open(path, encoding="utf-8") as f:
            sources[os.path.abspath(path)] = f.read()
    if argv[2:] and not only & set(sources):
        return 0  # nothing in scope was edited

    owned = one_spellings(root, sources)
    debt = load_debt(root)
    errors, notes = [], []
    uses = {}

    for path, src in sources.items():
        for lit in literals(src):
            for value, (name, owner) in owned.items():
                if path != owner and value in lit.text and (not only or path in only):
                    errors.append(f'{rel(root, path)}:{lit.line}: "{value}" is spelled once, as {name} in '
                                  f'{rel(root, owner)} — write ${name} (or ${{{name}}}) instead.')
            if len(lit.text) < MIN_LENGTH or letters(lit.text) < MIN_LETTERS:
                continue
            if ANNOTATION_ARG.search(lit.prefix):
                continue
            uses.setdefault(lit.text, []).append((path, lit.line))

    for text, where in sorted(uses.items()):
        allowed = max(MAX_COPIES, debt.get(text, 0))
        touched = not only or any(p in only for p, _ in where)
        if len(where) > allowed and touched:
            places = "\n".join(f"    {rel(root, p)}:{line}" for p, line in where)
            errors.append(f'"{text}" is written {len(where)} times (at most {allowed}):\n{places}\n'
                          f"  Name it once — a const val, or one function that owns the behaviour this "
                          f"text belongs to — and use that everywhere.")
        elif text in debt and len(where) < debt[text] and not only:
            notes.append(f'"{text}" is down to {len(where)} — lower its count in {DEBT_FILE}.')
    for text in debt:
        if text not in uses and not only:
            notes.append(f'"{text}" is gone — delete its line from {DEBT_FILE}.')

    for note in notes:
        print(f"note: {note}")
    if errors:
        for error in errors:
            print(f"error: {error}", file=sys.stderr)
        print(f"\nDuplicate text check failed. One spelling per meaning: see the header of "
              f"scripts/check_duplicate_literals.py.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
