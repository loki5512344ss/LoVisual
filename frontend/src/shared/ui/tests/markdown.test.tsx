import { render } from '@testing-library/react'
import { expect, test } from 'vitest'
import { Markdown } from '../Markdown'

test('renders headings, lists, inline styles and code', () => {
  const { container } = render(
    <Markdown>{'# Title\n\nSome **bold**, *em* and `code`.\n\n- one\n- two\n\n1. first\n2. second\n\n```\nlet x = 1\n```'}</Markdown>,
  )
  expect(container.querySelector('h1')).toHaveTextContent('Title')
  expect(container.querySelector('strong')).toHaveTextContent('bold')
  expect(container.querySelector('em')).toHaveTextContent('em')
  expect(container.querySelector('p code')).toHaveTextContent('code')
  expect(container.querySelectorAll('ul > li')).toHaveLength(2)
  expect(container.querySelectorAll('ol > li')).toHaveLength(2)
  expect(container.querySelector('pre code')).toHaveTextContent('let x = 1')
})

test('links keep http(s) targets and drop everything else', () => {
  const { container } = render(<Markdown>{'[ok](https://a.dev/x) [bad](javascript:alert(1)) https://b.dev'}</Markdown>)
  const hrefs = [...container.querySelectorAll('a')].map((a) => a.getAttribute('href'))
  expect(hrefs).toEqual(['https://a.dev/x', 'https://b.dev'])
  expect(container.textContent).toContain('bad')
})

test('raw HTML stays inert text', () => {
  const { container } = render(<Markdown>{'<img src=x onerror=alert(1)>\n\n<script>alert(1)</script>'}</Markdown>)
  expect(container.querySelector('img')).toBeNull()
  expect(container.querySelector('script')).toBeNull()
  expect(container.textContent).toContain('<script>')
})
