document.addEventListener('DOMContentLoaded', () => {
  // 1. Mobile Sidebar Drawer
  const menuToggle = document.querySelector('.menu-toggle');
  const sidebar = document.querySelector('.sidebar');

  if (menuToggle && sidebar) {
    menuToggle.addEventListener('click', (e) => {
      e.stopPropagation();
      sidebar.classList.toggle('open');
    });

    document.addEventListener('click', (e) => {
      if (sidebar.classList.contains('open') && !sidebar.contains(e.target)) {
        sidebar.classList.remove('open');
      }
    });
  }

  // 2. Active Sidebar Link Highlight
  const currentPath = window.location.pathname.replace(/\/$/, '');
  const sidebarLinks = document.querySelectorAll('.sidebar-menu a');

  sidebarLinks.forEach(link => {
    const linkPath = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '');
    if (linkPath === currentPath || (currentPath.endsWith('/docs') && linkPath.endsWith('index.html'))) {
      link.classList.add('active');
    }
  });

  // 3. Modern Copy Code to Clipboard
  const codeBlocks = document.querySelectorAll('pre');
  codeBlocks.forEach(pre => {
    const wrapper = pre.closest('.code-wrapper');
    if (!wrapper) return;

    const copyBtn = wrapper.querySelector('.copy-btn');
    if (!copyBtn) return;

    copyBtn.addEventListener('click', async () => {
      const code = pre.querySelector('code') || pre;
      const text = code.innerText;

      try {
        await navigator.clipboard.writeText(text);
        const originalText = copyBtn.innerHTML;
        copyBtn.innerHTML = '<span>✓ Copiado!</span>';
        copyBtn.classList.add('copied');

        setTimeout(() => {
          copyBtn.innerHTML = originalText;
          copyBtn.classList.remove('copied');
        }, 2000);
      } catch (err) {
        console.error('Falha ao copiar:', err);
      }
    });
  });

  // 4. Tab Switching Component (Tailwind Docs Style)
  const tabGroups = document.querySelectorAll('.tabs-group');
  tabGroups.forEach(group => {
    const buttons = group.querySelectorAll('.tab-btn');
    const panes = group.querySelectorAll('.tab-pane');

    buttons.forEach((btn, idx) => {
      btn.addEventListener('click', () => {
        buttons.forEach(b => b.classList.remove('active'));
        panes.forEach(p => p.classList.remove('active'));

        btn.classList.add('active');
        if (panes[idx]) {
          panes[idx].classList.add('active');
        }
      });
    });
  });

  // 5. Scrollspy for Table of Contents (TOC)
  const tocLinks = document.querySelectorAll('.toc-list a');
  if (tocLinks.length > 0) {
    const headings = Array.from(document.querySelectorAll('.content h2, .content h3'))
      .filter(h => h.id);

    const onScroll = () => {
      const scrollPos = window.scrollY + 100;
      let currentId = '';

      for (let i = 0; i < headings.length; i++) {
        if (headings[i].offsetTop <= scrollPos) {
          currentId = headings[i].id;
        }
      }

      tocLinks.forEach(link => {
        if (link.getAttribute('href') === `#${currentId}`) {
          link.classList.add('active');
        } else {
          link.classList.remove('active');
        }
      });
    };

    window.addEventListener('scroll', onScroll, { passive: true });
  }

  // 6. Keyboard Shortcut for Search (Ctrl+K / Cmd+K)
  document.addEventListener('keydown', (e) => {
    if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
      e.preventDefault();
      const searchBtn = document.querySelector('.search-btn');
      if (searchBtn) {
        searchBtn.click();
      }
    }
  });
});
