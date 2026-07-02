const DEFAULT_TOPIC_LABELS = {
  '\u5b66\u4e60\u65b9\u6cd5': {
    en: 'Study Methods',
    descriptionEn: 'Discuss study efficiency, time management, and review methods'
  },
  '\u7f16\u7a0b\u5165\u95e8': {
    en: 'Programming Basics',
    descriptionEn: 'Questions about learning Python, Java, frontend development, and more'
  },
  '\u8003\u7814\u5907\u8003': {
    en: 'Postgraduate Exam Prep',
    descriptionEn: 'Exam plans, resource choices, and progress management'
  },
  '\u82f1\u8bed\u5b66\u4e60': {
    en: 'English Learning',
    descriptionEn: 'Vocabulary, listening, reading, and writing practice'
  },
  'AI\u5de5\u5177': {
    en: 'AI Tools',
    descriptionEn: 'Use AI to support learning and planning'
  },
  '\u6570\u636e\u7ed3\u6784\u4e0e\u7b97\u6cd5': {
    en: 'Data Structures and Algorithms',
    descriptionEn: 'Discuss classic data structures, algorithms, and problem solving'
  },
  '\u64cd\u4f5c\u7cfb\u7edf': {
    en: 'Operating Systems',
    descriptionEn: 'Processes, threads, memory management, and OS fundamentals'
  },
  '\u8ba1\u7b97\u673a\u7f51\u7edc': {
    en: 'Computer Networks',
    descriptionEn: 'Network protocols, HTTP, TCP/IP, and distributed communication'
  },
  '\u524d\u7aef\u6846\u67b6\u5bf9\u6bd4': {
    en: 'Frontend Framework Comparison',
    descriptionEn: 'Compare Vue, React, and other frontend frameworks'
  },
  '\u673a\u5668\u5b66\u4e60\u5165\u95e8': {
    en: 'Machine Learning Basics',
    descriptionEn: 'Introductory machine learning concepts, models, and practice'
  },
  'Java\u540e\u7aef': {
    en: 'Java Backend',
    descriptionEn: 'Java backend development, Spring Boot, APIs, and services'
  }
}

function isEnglishLocale(locale) {
  return String(locale || '').toLowerCase().startsWith('en')
}

export function displayTopicName(topic, locale) {
  const name = typeof topic === 'string' ? topic : topic?.name
  if (!name) return ''
  if (!isEnglishLocale(locale)) return name
  const explicitEnglishName = topic?.name_en || topic?.nameEn || topic?.english_name || topic?.englishName
  if (explicitEnglishName) return explicitEnglishName
  return DEFAULT_TOPIC_LABELS[name]?.en || name
}

export function displayTopicDescription(topic, locale) {
  const name = topic?.name
  const description = topic?.description || ''
  if (!name || !isEnglishLocale(locale)) return description
  const explicitEnglishDescription = topic?.description_en || topic?.descriptionEn || topic?.english_description || topic?.englishDescription
  if (explicitEnglishDescription) return explicitEnglishDescription
  return DEFAULT_TOPIC_LABELS[name]?.descriptionEn || description
}

export function topicMatchesInput(topic, input, locale) {
  const normalizedInput = String(input || '').trim().toLowerCase()
  if (!normalizedInput) return false

  return [topic?.name, displayTopicName(topic, locale)]
    .filter(Boolean)
    .some((value) => String(value).trim().toLowerCase() === normalizedInput)
}
