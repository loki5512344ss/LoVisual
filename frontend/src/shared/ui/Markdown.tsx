import { Fragment, type ReactNode } from 'react'

/**
 * A small Markdown renderer for release notes: headings, paragraphs, bullet/numbered lists,
 * fenced code, quotes, rules, and inline bold/italic/code/links. It builds React elements
 * (never HTML strings) and treats raw HTML as plain text, so a release body cannot inject markup.
 * Links are limited to http(s)/mailto targets.
 */

const INLINE = /(`[^`\n]+`)|(\*\*[^*\n]+\*\*|__[^_\n]+__)|(\*[^*\n]+\*|_[^_\n]+_)|(\[[^\]\n]+\]\([^)\s]+\))|(https?:\/\/[^\s<>)]+)/g
const SAFE_URL = /^(https?:|mailto:)/i

function renderInline(text: string, keyBase: string): ReactNode[] {
  const out: ReactNode[] = []
  let last = 0
  let n = 0
  for (const m of text.matchAll(INLINE)) {
    const at = m.index
    if (at > last) out.push(text.slice(last, at))
    const [tok] = m
    const key = `${keyBase}-${n++}`
    if (m[1]) out.push(<code key={key}>{tok.slice(1, -1)}</code>)
    else if (m[2]) out.push(<strong key={key}>{renderInline(tok.slice(2, -2), key)}</strong>)
    else if (m[3]) out.push(<em key={key}>{renderInline(tok.slice(1, -1), key)}</em>)
    else if (m[4]) {
      const close = tok.indexOf('](')
      const label = tok.slice(1, close)
      const href = tok.slice(close + 2, -1)
      out.push(SAFE_URL.test(href) ? <a key={key} href={href} rel="noopener noreferrer" target="_blank">{label}</a> : label)
    } else out.push(<a key={key} href={tok} rel="noopener noreferrer" target="_blank">{tok}</a>)
    last = at + tok.length
  }
  if (last < text.length) out.push(text.slice(last))
  return out
}

const HEADING = /^(#{1,6})\s+(.*)$/
const BULLET = /^\s*[-*+]\s+(.*)$/
const NUMBERED = /^\s*\d+[.)]\s+(.*)$/
const RULE = /^\s*([-*_])(\s*\1){2,}\s*$/

function isBlockStart(line: string): boolean {
  return HEADING.test(line) || BULLET.test(line) || NUMBERED.test(line) || RULE.test(line) || line.startsWith('```') || line.startsWith('>')
}

export function Markdown({ children }: { children: string }) {
  const lines = children.replace(/\r\n?/g, '\n').split('\n')
  const blocks: ReactNode[] = []
  let i = 0

  while (i < lines.length) {
    const line = lines[i]
    const key = `b${blocks.length}`
    if (!line.trim()) {
      i++
    } else if (line.startsWith('```')) {
      const code: string[] = []
      i++
      while (i < lines.length && !lines[i].startsWith('```')) code.push(lines[i++])
      i++
      blocks.push(<pre key={key}><code>{code.join('\n')}</code></pre>)
    } else if (HEADING.test(line)) {
      const [, hashes, text] = HEADING.exec(line)!
      const Tag = `h${hashes.length}` as 'h1'
      blocks.push(<Tag key={key}>{renderInline(text.trim(), key)}</Tag>)
      i++
    } else if (RULE.test(line)) {
      blocks.push(<hr key={key} />)
      i++
    } else if (BULLET.test(line) || NUMBERED.test(line)) {
      const ordered = NUMBERED.test(line)
      const re = ordered ? NUMBERED : BULLET
      const items: string[] = []
      while (i < lines.length && re.test(lines[i])) items.push(re.exec(lines[i++])![1])
      const List = ordered ? 'ol' : 'ul'
      blocks.push(
        <List key={key}>
          {items.map((it, k) => (
            <li key={k}>{renderInline(it, `${key}-${k}`)}</li>
          ))}
        </List>,
      )
    } else if (line.startsWith('>')) {
      const quote: string[] = []
      while (i < lines.length && lines[i].startsWith('>')) quote.push(lines[i++].replace(/^>\s?/, ''))
      blocks.push(<blockquote key={key}>{renderInline(quote.join(' '), key)}</blockquote>)
    } else {
      const para: string[] = []
      while (i < lines.length && lines[i].trim() && !(para.length && isBlockStart(lines[i]))) para.push(lines[i++].trim())
      blocks.push(<p key={key}>{renderInline(para.join(' '), key)}</p>)
    }
  }

  return <Fragment>{blocks}</Fragment>
}
