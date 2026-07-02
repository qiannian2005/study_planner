import { marked } from 'marked'
import hljs from 'highlight.js'
import 'highlight.js/styles/github.min.css'

// 配置 marked
marked.setOptions({
  breaks: true,
  gfm: true,
  highlight: function(code, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return hljs.highlight(code, { language: lang }).value
      } catch (err) {
        console.error('代码高亮失败:', err)
      }
    }
    return hljs.highlightAuto(code).value
  }
})

// 自定义渲染器，增强安全性
const renderer = new marked.Renderer()

// 重写链接渲染，添加安全属性
renderer.link = function(href, title, text) {
  return `<a href="${href}" title="${title || ''}" target="_blank" rel="noopener noreferrer">${text}</a>`
}

marked.use({ renderer })

export function renderMarkdown(content) {
  if (!content) return ''
  return marked.parse(content)
}

export default marked

