import PptxGenJS from "/private/tmp/tradeflow-pptgen/node_modules/pptxgenjs/dist/pptxgen.cjs.js";

const pptx = new PptxGenJS();
pptx.layout = "LAYOUT_WIDE";
pptx.author = "OpenAI Codex";
pptx.company = "TradeFlow";
pptx.subject = "TradeFlow Hackathon Pitch";
pptx.title = "TradeFlow：AI驱动的跨境选品与套利分析工作台";
pptx.lang = "zh-CN";
pptx.theme = {
  headFontFace: "Aptos Display",
  bodyFontFace: "Aptos",
  lang: "zh-CN",
};

const colors = {
  bg: "0B1020",
  bg2: "121A31",
  panel: "111B35",
  panelSoft: "16213E",
  text: "F5F7FB",
  muted: "98A5C6",
  blue: "3FA7FF",
  cyan: "8BE9FD",
  green: "21C087",
  greenSoft: "183B39",
  red: "F26D6D",
  orange: "FFB454",
  border: "2B3A5B",
  white: "FFFFFF",
};

const page = { w: 13.333, h: 7.5 };

function setBg(slide) {
  slide.background = { color: colors.bg };
  slide.addShape(pptx.ShapeType.rect, {
    x: 0,
    y: 0,
    w: page.w,
    h: page.h,
    line: { color: colors.bg, transparency: 100 },
    fill: { color: colors.bg },
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: 8.6,
    y: -0.8,
    w: 5.6,
    h: 8.6,
    line: { color: colors.bg, transparency: 100 },
    fill: { color: colors.panelSoft, transparency: 35 },
    rotate: 18,
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: -0.5,
    y: 5.9,
    w: 5.8,
    h: 2.5,
    line: { color: colors.bg, transparency: 100 },
    fill: { color: colors.blue, transparency: 88 },
    rotate: -8,
  });
}

function addHeader(slide, kicker, title, subtitle) {
  slide.addText(kicker, {
    x: 0.65,
    y: 0.45,
    w: 3.6,
    h: 0.25,
    fontFace: "Aptos",
    fontSize: 11,
    bold: true,
    color: colors.cyan,
    charSpace: 1.6,
  });
  slide.addText(title, {
    x: 0.65,
    y: 0.8,
    w: 8.3,
    h: 0.8,
    fontFace: "Aptos Display",
    fontSize: 25,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  if (subtitle) {
    slide.addText(subtitle, {
      x: 0.68,
      y: 1.62,
      w: 7.8,
      h: 0.48,
      fontFace: "Aptos",
      fontSize: 12,
      color: colors.muted,
      margin: 0,
      breakLine: false,
    });
  }
}

function addFooter(slide, text = "TradeFlow Hackathon Pitch") {
  slide.addText(text, {
    x: 0.7,
    y: 7.08,
    w: 3.6,
    h: 0.2,
    fontSize: 8.5,
    color: "7D8CB3",
    margin: 0,
  });
  slide.addText("2026", {
    x: 12.0,
    y: 7.08,
    w: 0.6,
    h: 0.2,
    align: "right",
    fontSize: 8.5,
    color: "7D8CB3",
    margin: 0,
  });
}

function addChip(slide, text, x, y, w, fill, color = colors.text) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h: 0.36,
    rectRadius: 0.08,
    line: { color: fill, transparency: 100 },
    fill: { color: fill },
  });
  slide.addText(text, {
    x: x + 0.12,
    y: y + 0.075,
    w: w - 0.24,
    h: 0.18,
    fontSize: 10,
    bold: true,
    color,
    margin: 0,
    align: "center",
  });
}

function addPanel(slide, x, y, w, h, opts = {}) {
  slide.addShape(pptx.ShapeType.roundRect, {
    x,
    y,
    w,
    h,
    rectRadius: 0.12,
    line: { color: opts.border || colors.border, transparency: opts.borderTransparency ?? 10, width: 1 },
    fill: { color: opts.fill || colors.panel, transparency: opts.fillTransparency ?? 0 },
  });
}

function addBulletList(slide, items, x, y, w, opts = {}) {
  const bulletIndent = opts.bulletIndent ?? 16;
  const hanging = opts.hanging ?? 3;
  const textRuns = [];
  items.forEach((item, index) => {
    textRuns.push({
      text: item,
      options: {
        bullet: { indent: bulletIndent },
        hanging,
      },
    });
    if (index !== items.length - 1) {
      textRuns.push({ text: "\n" });
    }
  });
  slide.addText(textRuns, {
    x,
    y,
    w,
    h: opts.h || 2.3,
    fontFace: "Aptos",
    fontSize: opts.fontSize || 14,
    color: opts.color || colors.text,
    valign: "top",
    breakLine: false,
    paraSpaceAfterPt: opts.paraSpaceAfterPt || 10,
    margin: 0,
  });
}

function addMetricCard(slide, x, y, w, h, label, value, sub, accent = colors.blue) {
  addPanel(slide, x, y, w, h, { fill: colors.panel, border: colors.border });
  slide.addShape(pptx.ShapeType.rect, {
    x,
    y,
    w: 0.08,
    h,
    line: { color: accent, transparency: 100 },
    fill: { color: accent },
  });
  slide.addText(label, {
    x: x + 0.18,
    y: y + 0.18,
    w: w - 0.35,
    h: 0.2,
    fontSize: 10,
    bold: true,
    color: colors.muted,
    margin: 0,
  });
  slide.addText(value, {
    x: x + 0.18,
    y: y + 0.42,
    w: w - 0.35,
    h: 0.38,
    fontFace: "Aptos Display",
    fontSize: 20,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  if (sub) {
    slide.addText(sub, {
      x: x + 0.18,
      y: y + h - 0.34,
      w: w - 0.35,
      h: 0.16,
      fontSize: 8.5,
      color: colors.muted,
      margin: 0,
    });
  }
}

function addMiniWindow(slide, x, y, w, h, title, lines) {
  addPanel(slide, x, y, w, h, { fill: colors.panelSoft, border: colors.border });
  slide.addShape(pptx.ShapeType.rect, {
    x,
    y,
    w,
    h: 0.28,
    line: { color: colors.border, transparency: 100 },
    fill: { color: "0E1730" },
  });
  ["F26D6D", "FFB454", "21C087"].forEach((c, idx) => {
    slide.addShape(pptx.ShapeType.ellipse, {
      x: x + 0.16 + idx * 0.12,
      y: y + 0.09,
      w: 0.06,
      h: 0.06,
      line: { color: c, transparency: 100 },
      fill: { color: c },
    });
  });
  slide.addText(title, {
    x: x + 0.46,
    y: y + 0.065,
    w: w - 0.55,
    h: 0.12,
    fontSize: 8.5,
    color: colors.muted,
    margin: 0,
  });
  lines.forEach((line, idx) => {
    slide.addText(line, {
      x: x + 0.18,
      y: y + 0.42 + idx * 0.24,
      w: w - 0.36,
      h: 0.14,
      fontSize: idx === 0 ? 12 : 9.5,
      bold: idx === 0,
      color: idx === 0 ? colors.text : colors.muted,
      margin: 0,
    });
  });
}

function slide1() {
  const slide = pptx.addSlide();
  setBg(slide);

  slide.addShape(pptx.ShapeType.roundRect, {
    x: 0.68,
    y: 0.78,
    w: 0.82,
    h: 0.82,
    rectRadius: 0.08,
    line: { color: colors.blue, transparency: 100 },
    fill: { color: colors.blue },
  });
  slide.addText("TF", {
    x: 0.87,
    y: 1.02,
    w: 0.42,
    h: 0.18,
    fontFace: "Aptos Display",
    fontSize: 16,
    bold: true,
    color: colors.white,
    margin: 0,
    align: "center",
  });

  slide.addText("TradeFlow", {
    x: 1.68,
    y: 0.82,
    w: 4.7,
    h: 0.45,
    fontFace: "Aptos Display",
    fontSize: 24,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addText("AI驱动的跨境选品与套利分析工作台", {
    x: 0.72,
    y: 1.6,
    w: 7.2,
    h: 0.55,
    fontFace: "Aptos Display",
    fontSize: 28,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addText("把“海外爆款发现 -> 国内货源匹配 -> 利润测算 -> 风险判断”压缩成一个可执行的智能体流程。", {
    x: 0.75,
    y: 2.34,
    w: 6.8,
    h: 0.48,
    fontSize: 13.5,
    color: colors.muted,
    margin: 0,
  });

  addChip(slide, "Nuxt 4 + Spring Boot", 0.76, 3.1, 1.82, colors.panelSoft);
  addChip(slide, "GLM + BGE-M3 + pgvector", 2.72, 3.1, 2.3, colors.panelSoft);
  addChip(slide, "Hackathon Pitch 2026", 5.16, 3.1, 1.9, colors.greenSoft);

  addMiniWindow(slide, 8.25, 0.78, 4.25, 1.85, "Task Parameters", [
    "Acrylic Desktop Organizer",
    "AmazonUS / Target margin 30%",
    "Mode: Auto Fallback",
    "Start analysis",
  ]);
  addMiniWindow(slide, 8.25, 2.85, 2.0, 3.0, "Candidates", [
    "海外爆款候选",
    "Marker Holder",
    "Perfume Risers",
    "Paper Organizer",
    "9 results",
  ]);
  addMiniWindow(slide, 10.45, 2.85, 2.05, 3.0, "Report", [
    "结构化报告",
    "Profit +45.47",
    "Margin 48.6%",
    "Risk Low",
    "Decision Go",
  ]);

  slide.addText("黑客松路演版", {
    x: 0.75,
    y: 6.58,
    w: 2.0,
    h: 0.2,
    fontSize: 10,
    color: colors.cyan,
    margin: 0,
  });
  addFooter(slide);
}

function slide2() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "PROBLEM", "跨境选品最大的问题，不是找商品，而是快速做判断", "真正耗时的是跨平台搜索、语义转换、成本计算和风险判断的反复切换。");

  const cards = [
    ["平台切换成本高", "Amazon、1688、翻译、汇率、物流、表格来回跳转。"],
    ["语义鸿沟明显", "英文标题很难直接命中 1688 供货商品。"],
    ["利润测算不完整", "只看售价，忽略采购、运费、佣金和汇损。"],
    ["风险判断靠经验", "侵权、竞争和供货稳定性缺少结构化结论。"],
  ];
  cards.forEach((card, idx) => {
    const col = idx % 2;
    const row = Math.floor(idx / 2);
    const x = 0.8 + col * 3.45;
    const y = 2.15 + row * 1.75;
    addPanel(slide, x, y, 3.05, 1.35, { fill: colors.panelSoft });
    slide.addText(card[0], {
      x: x + 0.18,
      y: y + 0.16,
      w: 2.65,
      h: 0.2,
      fontSize: 16,
      bold: true,
      color: colors.text,
      margin: 0,
    });
    slide.addText(card[1], {
      x: x + 0.18,
      y: y + 0.5,
      w: 2.65,
      h: 0.5,
      fontSize: 10.5,
      color: colors.muted,
      margin: 0,
    });
  });

  addPanel(slide, 8.0, 2.1, 4.55, 3.8, { fill: "0E1730" });
  ["Amazon", "1688", "Translate", "FX", "Sheet"].forEach((label, idx) => {
    const x = idx % 2 === 0 ? 8.35 : 10.35;
    const y = 2.45 + Math.floor(idx / 2) * 1.0;
    addPanel(slide, x, y, 1.65, 0.68, { fill: colors.panelSoft });
    slide.addText(label, {
      x: x + 0.18,
      y: y + 0.23,
      w: 1.2,
      h: 0.14,
      fontSize: 12,
      bold: true,
      color: colors.text,
      margin: 0,
      align: "center",
    });
  });
  slide.addText("信息分散，判断断裂", {
    x: 8.42,
    y: 5.25,
    w: 3.8,
    h: 0.3,
    fontSize: 18,
    bold: true,
    color: colors.orange,
    margin: 0,
    align: "center",
  });
  addFooter(slide);
}

function slide3() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "SOLUTION", "TradeFlow 把选品流程收敛成“两阶段 AI 分析”", "保留用户选择权，但把最耗时的语义检索、成本计算和报告生成自动化。");

  const steps = [
    ["1", "关键词输入", "输入商品关键词、目标市场和利润率目标。"],
    ["2", "海外爆款发现", "先在海外平台筛出值得继续看的候选。"],
    ["3", "用户选中候选", "人机协同，在关键决策点由用户选择继续分析的商品。"],
    ["4", "国内货源匹配", "做中文改写、1688 语义召回和国内商品详情补齐。"],
    ["5", "利润与风险判断", "自动输出结构化报告、利润率、风险等级和建议动作。"],
  ];
  steps.forEach((step, idx) => {
    const x = 0.95 + idx * 2.45;
    const y = 2.45;
    slide.addShape(pptx.ShapeType.ellipse, {
      x,
      y,
      w: 0.52,
      h: 0.52,
      line: { color: colors.blue, transparency: 100 },
      fill: { color: idx === 4 ? colors.green : colors.blue },
    });
    slide.addText(step[0], {
      x: x + 0.16,
      y: y + 0.13,
      w: 0.18,
      h: 0.12,
      fontSize: 12,
      bold: true,
      color: colors.white,
      margin: 0,
      align: "center",
    });
    if (idx < steps.length - 1) {
      slide.addShape(pptx.ShapeType.chevron, {
        x: x + 0.7,
        y: 2.61,
        w: 0.5,
        h: 0.2,
        line: { color: colors.border, transparency: 100 },
        fill: { color: colors.border },
      });
    }
    slide.addText(step[1], {
      x: x - 0.12,
      y: 3.18,
      w: 1.8,
      h: 0.22,
      fontSize: 13,
      bold: true,
      color: colors.text,
      margin: 0,
    });
    slide.addText(step[2], {
      x: x - 0.12,
      y: 3.5,
      w: 1.92,
      h: 0.72,
      fontSize: 9.5,
      color: colors.muted,
      margin: 0,
    });
  });

  addMetricCard(slide, 1.0, 5.55, 2.9, 0.98, "输出不是一句回答", "结构化报告", "结论 / 利润率 / 风险 / 证据");
  addMetricCard(slide, 4.35, 5.55, 2.9, 0.98, "关键交互设计", "用户二次选择", "保留人机协同的可控性", colors.cyan);
  addMetricCard(slide, 7.7, 5.55, 4.0, 0.98, "系统价值", "闭环判断链路", "发现 + 匹配 + 测算 + 风控", colors.green);
  addFooter(slide);
}

function slide4() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "PRODUCT", "这不是聊天机器人，而是一套可直接操作的工作台", "三栏结构把任务参数、候选商品和结构化报告放在同一工作面上。");

  addPanel(slide, 0.8, 2.05, 2.65, 4.55, { fill: colors.panelSoft });
  slide.addText("任务参数", {
    x: 1.02,
    y: 2.28,
    w: 1.4,
    h: 0.18,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  ["Keyword", "Market", "Target Margin", "Top N", "Mode"].forEach((label, idx) => {
    addPanel(slide, 1.02, 2.72 + idx * 0.68, 2.2, 0.46, { fill: "0E1730" });
    slide.addText(label, {
      x: 1.14,
      y: 2.88 + idx * 0.68,
      w: 1.2,
      h: 0.1,
      fontSize: 9.5,
      color: colors.muted,
      margin: 0,
    });
  });
  addChip(slide, "开始分析", 1.1, 6.1, 1.1, colors.blue);

  addPanel(slide, 3.75, 2.05, 4.2, 4.55, { fill: colors.panel });
  slide.addText("海外爆款候选", {
    x: 4.0,
    y: 2.28,
    w: 1.8,
    h: 0.18,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  for (let i = 0; i < 6; i += 1) {
    const x = 4.0 + (i % 3) * 1.28;
    const y = 2.75 + Math.floor(i / 3) * 1.6;
    addPanel(slide, x, y, 1.05, 1.32, { fill: colors.panelSoft });
    slide.addShape(pptx.ShapeType.rect, {
      x: x + 0.12,
      y: y + 0.12,
      w: 0.81,
      h: 0.46,
      line: { color: colors.border, transparency: 100 },
      fill: { color: "233455" },
    });
    slide.addText(i === 0 ? "Acrylic\nMarker Holder" : "Candidate\nProduct", {
      x: x + 0.12,
      y: y + 0.66,
      w: 0.82,
      h: 0.3,
      fontSize: 8,
      bold: i === 0,
      color: colors.text,
      margin: 0,
      align: "center",
    });
  }

  addPanel(slide, 8.25, 2.05, 4.28, 4.55, { fill: colors.panelSoft });
  slide.addText("结构化报告", {
    x: 8.5,
    y: 2.28,
    w: 1.7,
    h: 0.18,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  addMetricCard(slide, 8.55, 2.7, 1.22, 0.92, "利润", "¥45.47", "预估利润", colors.green);
  addMetricCard(slide, 9.92, 2.7, 1.22, 0.92, "利润率", "48.6%", "预估利润率", colors.cyan);
  addMetricCard(slide, 11.29, 2.7, 0.96, 0.92, "风险", "低", "Low", colors.blue);
  addBulletList(slide, [
    "支持桌面工作台和流式浏览两种模式",
    "支持历史任务、报告列表、在线预览和 Markdown 下载",
    "思考日志和最终结论在同一界面汇总",
  ], 8.56, 3.95, 3.45, { h: 1.65, fontSize: 11.2, paraSpaceAfterPt: 8 });

  addFooter(slide);
}

function slide5() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "ARCHITECTURE", "TradeFlow 的壁垒在于把模型、检索、任务编排和 fallback 串成稳定链路", "不仅能回答问题，更能稳定地跑完一条业务流程。");

  const layers = [
    ["Frontend Workbench", "Nuxt 4 + Vue 3 + Pinia", 0.9, colors.blue],
    ["Task Orchestrator", "Spring Boot + 异步状态机", 3.45, colors.cyan],
    ["LLM Layer", "GLM 改写与报告叙事", 6.0, colors.orange],
    ["Vector Retrieval", "BGE-M3 + pgvector", 8.55, colors.green],
    ["Snapshots & Reports", "搜索快照 / 商品池 / 报告", 11.1, colors.blue],
  ];
  layers.forEach((layer) => {
    addPanel(slide, layer[2], 2.45, 1.95, 1.15, { fill: colors.panelSoft });
    slide.addShape(pptx.ShapeType.rect, {
      x: layer[2],
      y: 2.45,
      w: 1.95,
      h: 0.08,
      line: { color: layer[3], transparency: 100 },
      fill: { color: layer[3] },
    });
    slide.addText(layer[0], {
      x: layer[2] + 0.14,
      y: 2.75,
      w: 1.68,
      h: 0.18,
      fontSize: 12.2,
      bold: true,
      color: colors.text,
      margin: 0,
      align: "center",
    });
    slide.addText(layer[1], {
      x: layer[2] + 0.14,
      y: 3.05,
      w: 1.68,
      h: 0.24,
      fontSize: 8.5,
      color: colors.muted,
      margin: 0,
      align: "center",
    });
  });

  for (let i = 0; i < layers.length - 1; i += 1) {
    slide.addShape(pptx.ShapeType.chevron, {
      x: 2.95 + i * 2.55,
      y: 2.86,
      w: 0.28,
      h: 0.26,
      line: { color: colors.border, transparency: 100 },
      fill: { color: colors.border },
    });
  }

  addBulletList(slide, [
    "GLM 负责标题改写和报告叙事，解决语义转换和解释生成。",
    "BGE-M3 + pgvector 负责 1688 语义召回，提升中文货源匹配命中率。",
    "后端采用异步任务编排，天然适配长耗时智能体链路。",
    "真实接口不可用时自动回退到历史快照，保证 demo 稳定。"
  ], 0.95, 4.45, 6.0, { h: 1.9, fontSize: 12 });

  addMetricCard(slide, 8.2, 4.6, 1.3, 0.95, "LLM", "GLM", "rewrite + narrative", colors.orange);
  addMetricCard(slide, 9.65, 4.6, 1.3, 0.95, "Vector", "1024d", "BGE-M3", colors.green);
  addMetricCard(slide, 11.1, 4.6, 1.3, 0.95, "Mode", "Fallback", "stable demo", colors.blue);
  addFooter(slide);
}

function slide6() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "DEMO CASE", "用仓库里的真实 seed 数据，推导一个可讲的套利案例", "示例关键词：Acrylic Desktop Organizer。以下数字基于当前仓库的价格、运费和默认定价参数推导。");

  addPanel(slide, 0.85, 2.05, 3.3, 3.9, { fill: colors.panelSoft });
  slide.addText("Amazon 候选", {
    x: 1.1,
    y: 2.28,
    w: 1.5,
    h: 0.18,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: 1.08,
    y: 2.7,
    w: 2.84,
    h: 1.4,
    line: { color: colors.border, transparency: 100 },
    fill: { color: "203353" },
  });
  slide.addText("BEYGORM Magnetic Dry Erase Marker Holder", {
    x: 1.18,
    y: 4.28,
    w: 2.62,
    h: 0.38,
    fontSize: 12.5,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addText("售价 12.99 USD", {
    x: 1.18,
    y: 4.82,
    w: 1.3,
    h: 0.16,
    fontSize: 10.5,
    color: colors.cyan,
    margin: 0,
  });

  addPanel(slide, 4.55, 2.05, 3.3, 3.9, { fill: colors.panelSoft });
  slide.addText("1688 匹配货源", {
    x: 4.8,
    y: 2.28,
    w: 1.6,
    h: 0.18,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addShape(pptx.ShapeType.rect, {
    x: 4.78,
    y: 2.7,
    w: 2.84,
    h: 1.4,
    line: { color: colors.border, transparency: 100 },
    fill: { color: "203353" },
  });
  slide.addText("亚马逊爆款亚克力磁吸笔筒收纳盒", {
    x: 4.88,
    y: 4.28,
    w: 2.56,
    h: 0.38,
    fontSize: 12.5,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addText("采购 15.00 CNY / 国内运费 5.00 CNY", {
    x: 4.88,
    y: 4.82,
    w: 2.1,
    h: 0.16,
    fontSize: 10.5,
    color: colors.cyan,
    margin: 0,
  });

  addMetricCard(slide, 8.45, 2.15, 1.3, 1.0, "目标售价", "¥93.53", "12.99 USD x 7.20", colors.blue);
  addMetricCard(slide, 9.9, 2.15, 1.3, 1.0, "总成本", "¥48.06", "采购+运费+物流+佣金+汇损", colors.orange);
  addMetricCard(slide, 11.35, 2.15, 1.3, 1.0, "利润率", "48.6%", "当前配置推导", colors.green);
  addMetricCard(slide, 8.45, 3.45, 1.3, 1.0, "预估利润", "¥45.47", "真实样例数字", colors.green);
  addMetricCard(slide, 9.9, 3.45, 1.3, 1.0, "风险等级", "低", "可作为推荐案例", colors.cyan);
  addMetricCard(slide, 11.35, 3.45, 1.3, 1.0, "结论", "推荐进入", "适合 hackathon demo", colors.blue);

  addBulletList(slide, [
    "美元汇率 7.20",
    "跨境物流费率 12%",
    "平台佣金费率 15%",
    "汇损费率 3%",
  ], 8.48, 4.95, 3.6, { h: 1.05, fontSize: 11, paraSpaceAfterPt: 6 });
  addFooter(slide);
}

function slide7() {
  const slide = pptx.addSlide();
  setBg(slide);
  addHeader(slide, "DIFFERENTIATION", "为什么 TradeFlow 不是一个普通 AI Demo", "它更像一个最小可行产品，而不是只会回答问题的原型。");

  addPanel(slide, 0.95, 2.05, 5.45, 4.45, { fill: colors.panelSoft });
  addPanel(slide, 6.72, 2.05, 5.45, 4.45, { fill: colors.panel });
  slide.addText("普通 AI Demo", {
    x: 1.22,
    y: 2.32,
    w: 2.0,
    h: 0.18,
    fontSize: 18,
    bold: true,
    color: colors.orange,
    margin: 0,
  });
  slide.addText("TradeFlow", {
    x: 6.98,
    y: 2.32,
    w: 2.0,
    h: 0.18,
    fontSize: 18,
    bold: true,
    color: colors.green,
    margin: 0,
  });
  addBulletList(slide, [
    "停留在聊天回答或单点查询",
    "没有任务状态和过程日志",
    "不沉淀为结构化报告资产",
    "接口不稳时容易现场失效",
  ], 1.18, 2.82, 4.6, { h: 2.2, fontSize: 12.5 });
  addBulletList(slide, [
    "发现 + 匹配 + 测算 + 风控的完整闭环",
    "工作台式界面，适合真实业务操作",
    "异步任务、日志链路、报告下载都已具备",
    "通过历史快照和 fallback 保证 demo 稳定",
  ], 6.96, 2.82, 4.65, { h: 2.2, fontSize: 12.5 });

  addMetricCard(slide, 1.18, 5.5, 1.4, 0.82, "UI", "已完成", "三栏工作台");
  addMetricCard(slide, 2.76, 5.5, 1.4, 0.82, "Flow", "已打通", "两阶段任务", colors.cyan);
  addMetricCard(slide, 4.34, 5.5, 1.4, 0.82, "Report", "已输出", "结构化报告", colors.green);
  addMetricCard(slide, 6.96, 5.5, 1.4, 0.82, "LLM", "已接入", "GLM", colors.orange);
  addMetricCard(slide, 8.54, 5.5, 1.4, 0.82, "Vector", "已接入", "BGE-M3 + pgvector", colors.green);
  addMetricCard(slide, 10.12, 5.5, 1.4, 0.82, "Demo", "稳定", "fallback ready", colors.blue);
  addFooter(slide);
}

function slide8() {
  const slide = pptx.addSlide();
  setBg(slide);
  slide.addText("TradeFlow", {
    x: 0.85,
    y: 1.2,
    w: 3.4,
    h: 0.4,
    fontFace: "Aptos Display",
    fontSize: 30,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  slide.addText("让跨境选品从经验驱动，走向数据 + AI 驱动", {
    x: 0.86,
    y: 1.9,
    w: 7.6,
    h: 0.45,
    fontFace: "Aptos Display",
    fontSize: 24,
    bold: true,
    color: colors.cyan,
    margin: 0,
  });
  slide.addText("我们已经打通最难演示的一条链路：从海外爆款发现，到国内货源和利润判断。", {
    x: 0.88,
    y: 2.65,
    w: 7.1,
    h: 0.3,
    fontSize: 14,
    color: colors.muted,
    margin: 0,
  });

  addMetricCard(slide, 0.92, 4.25, 2.0, 1.06, "下一步 01", "扩市场", "接入更多海外站点", colors.blue);
  addMetricCard(slide, 3.12, 4.25, 2.0, 1.06, "下一步 02", "扩货源", "更多国内供货渠道", colors.cyan);
  addMetricCard(slide, 5.32, 4.25, 2.0, 1.06, "下一步 03", "扩风控", "更丰富风险信号", colors.green);

  addPanel(slide, 8.55, 1.12, 3.75, 5.4, { fill: colors.panelSoft });
  slide.addText("现场可演示", {
    x: 8.9,
    y: 1.55,
    w: 1.8,
    h: 0.2,
    fontSize: 17,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  addBulletList(slide, [
    "输入任意关键词",
    "生成海外候选列表",
    "选择一个候选做二次分析",
    "查看利润率、风险和最终建议",
  ], 8.9, 2.05, 2.9, { h: 2.0, fontSize: 12.5 });
  addChip(slide, "欢迎直接跑一个关键词 demo", 8.92, 5.55, 2.55, colors.greenSoft, colors.green);

  slide.addText("Thank you", {
    x: 0.92,
    y: 6.35,
    w: 2.4,
    h: 0.28,
    fontFace: "Aptos Display",
    fontSize: 18,
    bold: true,
    color: colors.text,
    margin: 0,
  });
  addFooter(slide);
}

slide1();
slide2();
slide3();
slide4();
slide5();
slide6();
slide7();
slide8();

await pptx.writeFile({ fileName: "/Users/coolmood/TradeFlow/docs/TradeFlow-黑客松路演.pptx" });
