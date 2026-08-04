(() => {
  const colors = [
    ['页面背景', '--color-canvas', '#0E2D42', 'rgb(14, 45, 66)'],
    ['底部导航', '--color-nav', '#1E2947', 'rgb(30, 41, 71)'],
    ['卡片表面', '--color-surface', '#334364', 'rgb(51, 67, 100)'],
    ['次级表面', '--color-surface-alt', '#575B8A', 'rgb(87, 91, 138)'],
    ['深色表面', '--color-surface-deep', '#0D0F35', 'rgb(13, 15, 53)'],
    ['主操作', '--color-action', '#FB6F30', 'rgb(251, 111, 48)'],
    ['强调操作', '--color-action-alt', '#FF653F', 'rgb(255, 101, 63)'],
    ['选中态', '--color-accent', '#6757D4', 'rgb(103, 87, 212)'],
    ['等级金', '--color-gold', '#E7C391', 'rgb(231, 195, 145)'],
    ['成功', '--color-success', '#8CDE4C', 'rgb(140, 222, 76)'],
    ['危险', '--color-danger', '#FA3747', 'rgb(250, 55, 71)'],
    ['错误文字', '--color-error', '#DD524D', 'rgb(221, 82, 77)'],
    ['输入表面', '--color-input', '#FDEBEB', 'rgb(253, 235, 235)'],
    ['主要文字', '--color-text', '#FFFFFF', 'rgb(255, 255, 255)'],
    ['次级文字', '--color-text-2', '#8189A0', 'rgb(129, 137, 160)'],
    ['弱化文字', '--color-text-3', '#999999', 'rgb(153, 153, 153)'],
    ['边框', '--color-border', '#292F3B', 'rgb(41, 47, 59)'],
    ['焦点', '--color-focus', '#3B3D66', 'rgb(59, 61, 102)'],
  ];

  const typeScale = [
    ['12px', '400', 'normal', '紧凑按钮 / 数据标签'],
    ['13px', '400', '17px', '导航 / 说明文字'],
    ['14px', '400', 'normal', '辅助内容'],
    ['16px', '400', '22.4px', '正文 / 输入 / 按钮'],
    ['17px', '400', 'normal', '登录 / 注册控件'],
    ['18px', '700', '25.2px', '页面标题'],
    ['25px', '500', 'normal', '登录主标题'],
    ['28px', '700', '34px', '注册主标题'],
  ];

  const spacing = [
    '2px',
    '5px',
    '8px',
    '11px',
    '12px',
    '17px',
    '22px',
    '28px',
    '34px',
  ];
  const radii = [
    ['4px', '小型表面'],
    ['9px', '操作按钮'],
    ['11px', '输入框'],
    ['13px', '注册控件'],
    ['17px', '宽操作按钮'],
    ['21px', '主要卡片'],
    ['28px', '胶囊按钮'],
    ['50%', '头像 / 圆形图标'],
  ];

  const assets = [
    ['品牌 Logo', 'assets/images/logo-novum.png'],
    ['登录背景', 'assets/images/login-background.png'],
    ['邀请横幅', 'assets/images/banner-invite.png'],
    ['APP 横幅', 'assets/images/banner-app-download.png'],
    ['功能入口', 'assets/images/feature-network.png'],
    ['功能入口', 'assets/images/feature-wallet.png'],
    ['News', 'assets/images/news-5g-city.jpg'],
    ['News', 'assets/images/news-ai-robot.png'],
    ['VIP', 'assets/images/vip-level-2-card.png'],
    ['空状态', 'assets/images/empty-state.png'],
    ['导航图标', 'assets/icons/icon-home-active.png'],
    ['功能图标', 'assets/icons/icon-service.png'],
  ];

  function append(tag, className, text) {
    const node = document.createElement(tag);
    if (className) node.className = className;
    if (text !== undefined) node.textContent = text;
    return node;
  }

  const colorGrid = document.querySelector('#color-grid');
  for (const [name, token, hex, rgb] of colors) {
    const item = append('article', 'color-item');
    const swatch = append('span', 'color-swatch');
    swatch.style.background = hex;
    const copy = append('div');
    copy.append(
      append('strong', '', name),
      append('code', '', token),
      append('small', '', `${hex} · ${rgb}`),
    );
    item.append(swatch, copy);
    colorGrid.append(item);
  }

  const typeContainer = document.querySelector('#type-scale');
  typeScale.forEach(([size, weight, lineHeight, use]) => {
    const row = append('div', 'type-row');
    const preview = append('span', '', 'Aa');
    preview.style.fontSize = size;
    preview.style.fontWeight = weight;
    preview.style.lineHeight = lineHeight;
    row.append(
      preview,
      append('code', '', `${size} / ${weight}`),
      append('small', '', use),
    );
    typeContainer.append(row);
  });

  const spacingContainer = document.querySelector('#spacing-scale');
  spacing.forEach((value, index) => {
    const row = append('div', 'space-row');
    row.append(append('code', '', `--space-${index + 1}`));
    const track = append('span', 'space-track');
    const bar = append('span', 'space-bar');
    bar.style.width = `${Number.parseFloat(value) * 5}px`;
    track.append(bar);
    row.append(track, append('span', '', value));
    spacingContainer.append(row);
  });

  const radiusContainer = document.querySelector('#radius-grid');
  radii.forEach(([value, use]) => {
    const item = append('div', 'radius-item');
    item.style.borderRadius = value;
    item.append(append('strong', '', value), append('span', '', use));
    radiusContainer.append(item);
  });

  const assetContainer = document.querySelector('#asset-gallery');
  assets.forEach(([name, path]) => {
    const item = append('figure', 'asset-item');
    const image = document.createElement('img');
    image.src = path;
    image.alt = name;
    image.loading = 'lazy';
    item.append(image, append('figcaption', '', name));
    assetContainer.append(item);
  });

  const tokenLines = [
    ':root {',
    ...colors.map(([, token, hex]) => `  ${token}: ${hex};`),
    '  --font-default: Times, "Times New Roman", serif;',
    ...spacing.map((value, index) => `  --space-${index + 1}: ${value};`),
    ...radii
      .slice(0, -1)
      .map(([value], index) => `  --radius-${index + 1}: ${value};`),
    '  --shadow-soft: 0 1px 5px rgb(0 0 0 / 5%);',
    '}',
  ];
  document.querySelector('#token-code').textContent = tokenLines.join('\n');

  const links = [...document.querySelectorAll('.section-nav a')];
  const sections = links
    .map((link) => document.querySelector(link.getAttribute('href')))
    .filter(Boolean);
  const observer = new IntersectionObserver(
    (entries) => {
      const visible = entries.find((entry) => entry.isIntersecting);
      if (!visible) return;
      links.forEach((link) =>
        link.toggleAttribute(
          'aria-current',
          link.getAttribute('href') === `#${visible.target.id}`,
        ),
      );
    },
    { rootMargin: '-20% 0px -70% 0px' },
  );
  sections.forEach((section) => observer.observe(section));
})();
