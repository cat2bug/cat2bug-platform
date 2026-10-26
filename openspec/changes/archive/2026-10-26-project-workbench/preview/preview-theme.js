(function () {
  var KEY = 'project-workbench-preview-theme'
  function apply(mode) {
    var dark = mode === 'dark'
    document.documentElement.classList.toggle('dark', dark)
    document.querySelectorAll('[data-theme-btn]').forEach(function (btn) {
      btn.classList.toggle('on', btn.getAttribute('data-theme-btn') === mode)
    })
    try { localStorage.setItem(KEY, mode) } catch (e) {}
  }
  function current() {
    try {
      var saved = localStorage.getItem(KEY)
      if (saved === 'dark' || saved === 'light') return saved
    } catch (e) {}
    return 'light'
  }
  document.addEventListener('DOMContentLoaded', function () {
    apply(current())
    document.querySelectorAll('[data-theme-btn]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        apply(btn.getAttribute('data-theme-btn'))
      })
    })
  })
})()
