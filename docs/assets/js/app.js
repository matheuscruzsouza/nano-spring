document.addEventListener('DOMContentLoaded', () => {
  // Mobile Sidebar Toggle
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

  // Active Link Highlight
  const currentPath = window.location.pathname.replace(/\/$/, '');
  const sidebarLinks = document.querySelectorAll('.sidebar-menu a');

  sidebarLinks.forEach(link => {
    const linkPath = new URL(link.href, window.location.origin).pathname.replace(/\/$/, '');
    if (linkPath === currentPath || (currentPath.endsWith('/docs') && linkPath.endsWith('index.html'))) {
      link.classList.add('active');
    }
  });

  // Copy Code to Clipboard
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
        copyBtn.textContent = '✓ Copiado!';
        copyBtn.classList.add('copied');

        setTimeout(() => {
          copyBtn.textContent = 'Copiar';
          copyBtn.classList.remove('copied');
        }, 2000);
      } catch (err) {
        console.error('Falha ao copiar:', err);
      }
    });
  });
});
