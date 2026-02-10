export function formatJson(input: string): { ok: boolean; value: string; error?: string } {
  try {
    const parsed = JSON.parse(input);
    return { ok: true, value: JSON.stringify(parsed, null, 2) };
  } catch (err) {
    return { ok: false, value: input, error: (err as Error).message };
  }
}

export function validateJson(input: string): string | null {
  try {
    JSON.parse(input);
    return null;
  } catch (err) {
    return (err as Error).message;
  }
}

export function validateJs(input: string): string | null {
  try {
    new Function(input);
    return null;
  } catch (err) {
    return (err as Error).message;
  }
}

export function validateXml(input: string): string | null {
  try {
    const parser = new DOMParser();
    const doc = parser.parseFromString(input, 'application/xml');
    const parseError = doc.querySelector('parsererror');
    if (parseError) {
      return parseError.textContent || 'Invalid XML';
    }
    return null;
  } catch (err) {
    return (err as Error).message;
  }
}

type JsScanState = {
  inSingle: boolean;
  inDouble: boolean;
  inTemplate: boolean;
  inBlockComment: boolean;
};

export function formatJs(input: string): { ok: boolean; value: string; error?: string } {
  try {
    const normalized = input.replace(/\r\n?/g, '\n');
    const lines = normalized.split('\n');
    const state: JsScanState = {
      inSingle: false,
      inDouble: false,
      inTemplate: false,
      inBlockComment: false
    };
    let indentLevel = 0;

    const formatted = lines.map((line) => {
      if (!line.trim()) {
        return '';
      }

      const scan = scanJsLine(line, state);
      const lineIndent = scan.startsWithClose ? Math.max(indentLevel - 1, 0) : indentLevel;
      indentLevel = Math.max(0, indentLevel + scan.openBraces - scan.closeBraces);

      return `${'  '.repeat(lineIndent)}${line.trim()}`;
    });

    return { ok: true, value: formatted.join('\n') };
  } catch (err) {
    return { ok: false, value: input, error: (err as Error).message };
  }
}

export function highlightJs(input: string): string {
  if (!input) {
    return '';
  }

  return highlightSegment(input);
}

function scanJsLine(
  line: string,
  state: JsScanState
): { openBraces: number; closeBraces: number; startsWithClose: boolean } {
  let openBraces = 0;
  let closeBraces = 0;
  let startsWithClose = false;
  let sawToken = false;
  let inLineComment = false;
  let escaped = false;

  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    const next = line[i + 1];

    if (inLineComment) {
      break;
    }

    if (state.inBlockComment) {
      if (char === '*' && next === '/') {
        state.inBlockComment = false;
        i += 1;
      }
      continue;
    }

    if (state.inSingle) {
      if (!escaped && char === "'") {
        state.inSingle = false;
      }
      escaped = char === '\\' && !escaped;
      continue;
    }

    if (state.inDouble) {
      if (!escaped && char === '"') {
        state.inDouble = false;
      }
      escaped = char === '\\' && !escaped;
      continue;
    }

    if (state.inTemplate) {
      if (!escaped && char === '`') {
        state.inTemplate = false;
      }
      escaped = char === '\\' && !escaped;
      continue;
    }

    if (char === '/' && next === '/') {
      inLineComment = true;
      continue;
    }

    if (char === '/' && next === '*') {
      state.inBlockComment = true;
      i += 1;
      continue;
    }

    if (char === "'") {
      state.inSingle = true;
      escaped = false;
      continue;
    }

    if (char === '"') {
      state.inDouble = true;
      escaped = false;
      continue;
    }

    if (char === '`') {
      state.inTemplate = true;
      escaped = false;
      continue;
    }

    if (!sawToken && !/\s/.test(char)) {
      sawToken = true;
      startsWithClose = char === '}';
    }

    if (char === '{') {
      openBraces += 1;
    } else if (char === '}') {
      closeBraces += 1;
    }
  }

  return { openBraces, closeBraces, startsWithClose };
}

function highlightSegment(input: string): string {
  let result = '';
  let index = 0;
  let lastPlainStart = 0;

  while (index < input.length) {
    const char = input[index];
    const next = input[index + 1];

    if (char === '/' && next === '/') {
      result += highlightPlain(input.slice(lastPlainStart, index));
      let end = input.indexOf('\n', index + 2);
      if (end === -1) {
        end = input.length;
      } else {
        end += 1;
      }
      result += wrapToken('comment', input.slice(index, end));
      index = end;
      lastPlainStart = end;
      continue;
    }

    if (char === '/' && next === '*') {
      result += highlightPlain(input.slice(lastPlainStart, index));
      let end = input.indexOf('*/', index + 2);
      if (end === -1) {
        end = input.length;
      } else {
        end += 2;
      }
      result += wrapToken('comment', input.slice(index, end));
      index = end;
      lastPlainStart = end;
      continue;
    }

    if (char === "'" || char === '"') {
      result += highlightPlain(input.slice(lastPlainStart, index));
      const end = readString(input, index, char);
      result += wrapToken('string', input.slice(index, end));
      index = end;
      lastPlainStart = end;
      continue;
    }

    if (char === '`') {
      result += highlightPlain(input.slice(lastPlainStart, index));
      const template = highlightTemplateLiteral(input, index);
      result += template.html;
      index = template.endIndex;
      lastPlainStart = index;
      continue;
    }

    if (char === '/' && isRegexStart(input, index)) {
      result += highlightPlain(input.slice(lastPlainStart, index));
      const end = readRegexLiteral(input, index);
      result += wrapToken('regex', input.slice(index, end));
      index = end;
      lastPlainStart = end;
      continue;
    }

    index += 1;
  }

  if (lastPlainStart < input.length) {
    result += highlightPlain(input.slice(lastPlainStart));
  }

  return result;
}

function highlightTemplateLiteral(input: string, start: number): { html: string; endIndex: number } {
  let output = wrapToken('string', '`');
  let index = start + 1;
  let segmentStart = index;

  while (index < input.length) {
    const char = input[index];
    const next = input[index + 1];

    if (char === '\\') {
      index += 2;
      continue;
    }

    if (char === '`') {
      output += wrapToken('string', input.slice(segmentStart, index));
      output += wrapToken('string', '`');
      return { html: output, endIndex: index + 1 };
    }

    if (char === '$' && next === '{') {
      output += wrapToken('string', input.slice(segmentStart, index));
      output += wrapToken('punctuation', '${');
      const exprStart = index + 2;
      const exprEnd = readTemplateExpression(input, exprStart);
      if (exprEnd >= input.length) {
        output += highlightSegment(input.slice(exprStart));
        return { html: output, endIndex: input.length };
      }
      output += highlightSegment(input.slice(exprStart, exprEnd));
      output += wrapToken('punctuation', '}');
      index = exprEnd + 1;
      segmentStart = index;
      continue;
    }

    index += 1;
  }

  output += wrapToken('string', input.slice(segmentStart));
  return { html: output, endIndex: input.length };
}

function readTemplateExpression(input: string, start: number): number {
  let index = start;
  let depth = 1;
  let inSingle = false;
  let inDouble = false;
  let inLineComment = false;
  let inBlockComment = false;
  let templateMode = false;
  let escaped = false;
  const templateReturnDepths: number[] = [];

  while (index < input.length) {
    const char = input[index];
    const next = input[index + 1];

    if (templateMode) {
      if (char === '\\') {
        index += 2;
        continue;
      }
      if (char === '`') {
        templateMode = false;
        index += 1;
        continue;
      }
      if (char === '$' && next === '{') {
        depth += 1;
        templateReturnDepths.push(depth);
        templateMode = false;
        index += 2;
        continue;
      }
      index += 1;
      continue;
    }

    if (inLineComment) {
      if (char === '\n') {
        inLineComment = false;
      }
      index += 1;
      continue;
    }

    if (inBlockComment) {
      if (char === '*' && next === '/') {
        inBlockComment = false;
        index += 2;
      } else {
        index += 1;
      }
      continue;
    }

    if (inSingle) {
      if (!escaped && char === "'") {
        inSingle = false;
      }
      escaped = char === '\\' && !escaped;
      index += 1;
      continue;
    }

    if (inDouble) {
      if (!escaped && char === '"') {
        inDouble = false;
      }
      escaped = char === '\\' && !escaped;
      index += 1;
      continue;
    }

    if (char === '/' && next === '/') {
      inLineComment = true;
      index += 2;
      continue;
    }

    if (char === '/' && next === '*') {
      inBlockComment = true;
      index += 2;
      continue;
    }

    if (char === "'") {
      inSingle = true;
      escaped = false;
      index += 1;
      continue;
    }

    if (char === '"') {
      inDouble = true;
      escaped = false;
      index += 1;
      continue;
    }

    if (char === '`') {
      templateMode = true;
      index += 1;
      continue;
    }

    if (char === '/' && isRegexStart(input, index)) {
      index = readRegexLiteral(input, index);
      continue;
    }

    if (char === '{') {
      depth += 1;
      index += 1;
      continue;
    }

    if (char === '}') {
      depth -= 1;
      if (depth === 0) {
        return index;
      }
      if (templateReturnDepths.length && depth < templateReturnDepths[templateReturnDepths.length - 1]) {
        templateReturnDepths.pop();
        templateMode = true;
      }
      index += 1;
      continue;
    }

    index += 1;
  }

  return input.length;
}

function readRegexLiteral(input: string, start: number): number {
  let index = start + 1;
  let escaped = false;
  let inClass = false;

  while (index < input.length) {
    const char = input[index];
    if (escaped) {
      escaped = false;
      index += 1;
      continue;
    }
    if (char === '\\') {
      escaped = true;
      index += 1;
      continue;
    }
    if (char === '[') {
      inClass = true;
      index += 1;
      continue;
    }
    if (char === ']' && inClass) {
      inClass = false;
      index += 1;
      continue;
    }
    if (char === '/' && !inClass) {
      index += 1;
      while (index < input.length && /[a-z]/i.test(input[index])) {
        index += 1;
      }
      return index;
    }
    index += 1;
  }

  return input.length;
}

function isRegexStart(input: string, index: number): boolean {
  let i = index - 1;
  while (i >= 0 && /\s/.test(input[i])) {
    i -= 1;
  }

  if (i < 0) {
    return true;
  }

  const prev = input[i];
  if (/[({[;,=:!?&|+\-*/%^~<>]/.test(prev)) {
    return true;
  }

  if (prev === '.' || prev === ')' || prev === ']' || prev === '}') {
    return false;
  }

  if (/[A-Za-z0-9_$]/.test(prev)) {
    const match = /([A-Za-z_$][\w$]*)\s*$/.exec(input.slice(0, index));
    if (match) {
      return REGEX_KEYWORDS.has(match[1]);
    }
    return false;
  }

  return true;
}

function readString(input: string, start: number, quote: string): number {
  let escaped = false;
  for (let i = start + 1; i < input.length; i += 1) {
    const char = input[i];
    if (!escaped && char === quote) {
      return i + 1;
    }
    if (!escaped && char === '\\') {
      escaped = true;
      continue;
    }
    escaped = false;
  }
  return input.length;
}

function wrapToken(className: string, value: string): string {
  return `<span class="token ${className}">${escapeHtml(value)}</span>`;
}

const REGEX_KEYWORDS = new Set([
  'return',
  'throw',
  'case',
  'else',
  'do',
  'typeof',
  'delete',
  'void',
  'new',
  'in',
  'instanceof',
  'of',
  'await',
  'yield'
]);

function highlightPlain(value: string): string {
  if (!value) {
    return '';
  }

  const escaped = escapeHtml(value);
  const tokenRegex =
    /\b(?:break|case|catch|class|const|continue|debugger|default|delete|do|else|export|extends|finally|for|function|if|import|in|instanceof|let|new|return|super|switch|this|throw|try|typeof|var|void|while|with|yield|async|await|of|true|false|null|undefined)\b|\b0x[0-9a-fA-F]+\b|\b\d+(?:\.\d+)?(?:e[+-]?\d+)?\b/g;
  let result = '';
  let lastIndex = 0;

  for (const match of escaped.matchAll(tokenRegex)) {
    if (match.index === undefined) {
      continue;
    }
    result += escaped.slice(lastIndex, match.index);
    const token = match[0];
    const className = token[0].toLowerCase() === '0' || /\d/.test(token[0]) ? 'number' : 'keyword';
    result += `<span class="token ${className}">${token}</span>`;
    lastIndex = match.index + token.length;
  }

  result += escaped.slice(lastIndex);
  return result;
}

function escapeHtml(value: string): string {
  return value.replace(/[&<>]/g, (char) => {
    switch (char) {
      case '&':
        return '&amp;';
      case '<':
        return '&lt;';
      case '>':
        return '&gt;';
      default:
        return char;
    }
  });
}
