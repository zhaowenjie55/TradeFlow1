import type { DomesticProductMatch, TaskLogEntry } from '~/types'

const ENGLISH_NOISE_WORDS = new Set([
  'for',
  'with',
  'and',
  'the',
  'a',
  'an',
  'of',
  'to',
  'from',
  'on',
  'in',
  'clear',
  'modern',
  'desktop',
  'desk',
  'organizer',
  'holder',
  'storage',
  'accessories',
  'supplies',
])

const CATEGORY_PATTERNS: Array<{ regex: RegExp, label: string }> = [
  { regex: /(dry erase|whiteboard|marker holder|magnetic.+pen holder|magnet.+marker)/i, label: '磁吸白板笔收纳盒' },
  { regex: /((paper|letter|file).+(organizer|tray))|((organizer|tray).+(paper|letter|file))/i, label: '亚克力文件盘' },
  { regex: /((perfume|display|riser|cupcake).+(organizer|stand|riser))|display risers/i, label: '亚克力展示架' },
  { regex: /((makeup|cosmetic|skincare).+(organizer|drawer))|organizer.+drawer/i, label: '透明美妆收纳盒' },
  { regex: /((pencil|pen).+(holder|cup))|stationery organizer/i, label: '亚克力笔筒' },
  { regex: /(desktop organizer|desk organizer|locker organizer)/i, label: '桌面收纳架' },
]

const LOG_STAGE_LABELS: Record<string, string> = {
  'phase1.create': '任务创建',
  'phase1.queue': '等待执行',
  'phase1.market-scan': '海外样本检索',
  'phase1.filter': '候选筛选',
  'phase1.fallback': '历史结果回退',
  'phase1.output': '候选输出',
  'phase2.create': '二阶段接管',
  'phase2.queue': '等待执行',
  'phase2.domestic-match': '国内货源比对',
  'phase2.rewrite': '检索词整理',
  'phase2.domestic-search': '同款搜索',
  'phase2.product-detail': '详情补齐',
  'phase2.pricing': '利润测算',
  'phase2.report': '结论整理',
}

const LIVE_STAGE_MESSAGES: Record<string, string> = {
  'phase1.create': '正在登记任务并准备拉取候选数据',
  'phase1.queue': '任务已进入队列，等待执行',
  'phase1.market-scan': '正在抓取海外样本并整理热度信号',
  'phase1.filter': '正在压缩候选范围，保留更值得继续看的商品',
  'phase1.fallback': '实时链路不稳定，正在拼接历史样本兜底',
  'phase1.output': '正在收束一阶段结果并准备展示候选',
  'phase2.create': '二阶段已接管，准备围绕当前商品展开深挖',
  'phase2.queue': '二阶段任务已排队，马上开始比对',
  'phase2.domestic-match': '正在同步国内货源、价格和利润空间',
  'phase2.rewrite': '正在压缩标题重点并生成更像买家会搜的检索词',
  'phase2.domestic-search': '正在跑关键词检索和语义召回，筛掉偏题货源',
  'phase2.product-detail': '正在补齐品牌、规格和属性细节',
  'phase2.pricing': '正在重算成本、平台抽佣、物流和利润率',
  'phase2.report': '正在把零散证据整理成最终结论',
}

const containsChinese = (value?: string | null) => Boolean(value && /[\u4e00-\u9fa5]/.test(value))

const trimPunctuation = (value: string) => value.replace(/^[\s\-_,./]+|[\s\-_,./]+$/g, '').trim()

const buildModifierPrefix = (title: string) => {
  const modifiers: string[] = []

  if (/\b4\s*(tier|tray|layer)s?\b/i.test(title)) modifiers.push('四层')
  else if (/\b3\s*(tier|tray|layer)s?\b/i.test(title)) modifiers.push('三层')
  else if (/\b2\s*(tier|tray|layer)s?\b/i.test(title)) modifiers.push('双层')

  if (/\b2\s*pack\b/i.test(title)) modifiers.push('双只装')
  return modifiers.join('')
}

const simplifyChineseHint = (value?: string | null) => {
  if (!value) return ''

  const segments = value
    .replace(/[【】]/g, ' ')
    .split(/[|｜/、，,；;：:\-（(]/)
    .map(item => trimPunctuation(item))
    .filter(Boolean)

  const preferred = segments.find(item => /(收纳|置物|展示|笔筒|文件|托盘|白板笔|香水|化妆|亚克力)/.test(item))
  const candidate = preferred ?? segments[0] ?? value

  return candidate.length > 16 ? `${candidate.slice(0, 16)}...` : candidate
}

const simplifyEnglishTitle = (title: string) => {
  const tokens = title
    .replace(/[,_/()-]+/g, ' ')
    .split(/\s+/)
    .map(item => item.trim())
    .filter(Boolean)

  const filtered = tokens.filter((token, index) => {
    if (index === 0 && /^[A-Z0-9-]{4,}$/.test(token)) return false
    return !ENGLISH_NOISE_WORDS.has(token.toLowerCase())
  })

  const words = (filtered.length ? filtered : tokens).slice(0, 5)
  return words.join(' ').trim() || '商品'
}

export const getReadableProductName = (
  title?: string | null,
  options?: {
    domesticHint?: string | null
  },
) => {
  if (!title) return '商品'

  const chineseHint = simplifyChineseHint(options?.domesticHint)
  if (containsChinese(chineseHint)) return chineseHint

  const matchedCategory = CATEGORY_PATTERNS.find(item => item.regex.test(title))
  if (matchedCategory) {
    return `${buildModifierPrefix(title)}${matchedCategory.label}` || matchedCategory.label
  }

  return simplifyEnglishTitle(title)
}

export const getReadableOriginalTitle = (title?: string | null, readableName?: string | null) => {
  if (!title) return ''
  if (!readableName || readableName === title) return ''
  return title.length > 96 ? `${title.slice(0, 96)}...` : title
}

export const getReadableMatchTitle = (match: DomesticProductMatch) => {
  return getReadableProductName(match.title, { domesticHint: match.title })
}

export const getReportFileName = (title?: string | null, domesticHint?: string | null) => {
  const base = getReadableProductName(title, { domesticHint })
    .replace(/[<>:"/\\|?*\x00-\x1F]/g, '')
    .trim()

  return `${base || '商品'}-套利报告.md`
}

export const compactNarrativeSummary = (
  message?: string | null,
  options?: {
    productTitle?: string | null
    domesticTitle?: string | null
  },
) => {
  if (!message) return ''

  const productName = getReadableProductName(options?.productTitle, { domesticHint: options?.domesticTitle })
  const domesticName = getReadableProductName(options?.domesticTitle, { domesticHint: options?.domesticTitle })

  let normalized = message.replace(/\s+/g, ' ').trim()

  if (options?.productTitle) {
    normalized = normalized.replaceAll(options.productTitle, productName)
  }

  if (options?.domesticTitle) {
    normalized = normalized.replaceAll(options.domesticTitle, domesticName)
  }

  normalized = normalized
    .replace(/^Agent\s*结合候选商品、历史货源和成本拆解后判断，?/i, '')
    .replace(/当前更适合作为可解释、可演示的机会案例，?/g, '适合作为重点跟进案例，')
    .replace(/主要对标货源为/g, '对标货源为')

  const tightened = normalized.match(/^(.*?)(在 .*?站点当前具备约 .*? 的利润空间，)(.*?最终预估利润约为 .*?[。.]?)/)
  if (tightened) {
    const namePart = tightened[1].trim().replace(/[，,]$/, '')
    const marginPart = tightened[2].trim()
    const endingPart = tightened[3].trim()
    return `${namePart}，${marginPart}${endingPart}`
  }

  return normalized
}

export const humanizeTaskLog = (entry: TaskLogEntry) => {
  const raw = entry.message.trim()
  let message = raw

  if (/Phase1 任务已创建并进入队列/.test(raw)) {
    message = '任务已经建好，先去拉一轮海外候选数据。'
  } else if (/开始执行商品库驱动的 Amazon 候选检索/.test(raw)) {
    message = '先从海外样本里扫一遍相关商品，把能做候选的结果抓出来。'
  } else if (/Amazon 商品库未命中/.test(raw)) {
    message = '实时结果不够，我先切到历史快照补齐样本，避免候选直接断掉。'
  } else if (/已按关键词相似度/.test(raw)) {
    message = '候选已经按相关度、排序位置、口碑和价格带重新压了一遍。'
  } else if (/候选商品已生成/.test(raw)) {
    message = '第一批候选已经整理完，可以继续挑一个做深度分析。'
  } else if (/Phase2 任务已创建/.test(raw)) {
    message = '接管你选中的商品，开始往国内货源、成本和利润链路继续深挖。'
  } else if (/开始执行国内货源匹配与 ROI 试算/.test(raw)) {
    message = '先把国内供货、运费和 ROI 框架同时跑起来，看看这单值不值得追。'
  } else if (/已完成真实 GLM 改写/.test(raw)) {
    message = '先压缩出标题里的核心卖点，再生成一组更贴近国内搜索习惯的检索词。'
  } else if (/已完成商品库混合检索与语义匹配/.test(raw)) {
    message = '关键词检索和语义召回已经一起跑完，正在保留更像同款的候选。'
  } else if (/已加载商品详情快照/.test(raw)) {
    message = '详情快照已经补齐，品牌、规格和属性可以一起纳入判断。'
  } else if (/未命中商品详情快照/.test(raw)) {
    message = '详情没有完全命中，我先用标题、价格和检索证据继续往下推。'
  } else if (/已完成成本公式测算与利润估算/.test(raw)) {
    message = '成本、汇率、平台抽佣和物流都过了一遍，利润空间已经基本收口。'
  } else if (/结构化报告已生成/.test(raw)) {
    message = '结论已经整理完成，右侧报告现在可以直接看。'
  }

  return {
    stageLabel: LOG_STAGE_LABELS[entry.stage] ?? entry.stage,
    message,
  }
}

export const resolveTaskLogCategory = (entry: TaskLogEntry) => {
  if (entry.level === 'ERROR' || entry.level === 'WARN' || entry.level === 'WARNING') return 'alert'
  if (entry.stage.includes('create') || entry.stage.includes('queue') || entry.stage.includes('fallback') || entry.stage.includes('output')) return 'system'
  return 'agent'
}

export const isKeyTaskLog = (entry: TaskLogEntry) => {
  return resolveTaskLogCategory(entry) !== 'system' || entry.level === 'ERROR'
}

export const getLiveStageMessage = (stage?: string | null) => {
  if (!stage) return '正在继续补充分析过程'
  return LIVE_STAGE_MESSAGES[stage] ?? '正在继续补充分析过程'
}
