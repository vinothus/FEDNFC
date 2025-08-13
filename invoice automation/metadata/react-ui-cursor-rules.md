# ✨ React UI Development Cursor Rules

This document defines rules and standards for building high-quality React applications with a strong emphasis on responsiveness, accessibility, and maintainability.

---

## 📦 0. General Command Usage

> ⚠️ **Always use CLI commands to generate components, controllers, and other files.** Do not manually create files as it increases the chance of human error.

For example:

```bash
npx create-react-app my-app
npx generate-react-cli component MyComponent
npx generate-react-cli container MyContainer
```
Or use your project's specific scaffolding commands (e.g., `nx`, `vite`, `next`, etc.).

---

## 📱 1. Responsiveness Rules

- ✅ Use Flexbox or Grid for layouts.
- ✅ Design mobile-first, then scale up.
- ✅ Avoid hardcoded pixel widths; prefer `%`, `vw`, or utility classes like Tailwind's `w-full`, `max-w-screen-lg`, etc.
- ✅ Always test at breakpoints: `360px`, `768px`, `1024px`, `1440px`.
- ✅ Use Tailwind or CSS media queries for responsive behavior.

---

## ♿ 2. Accessibility (a11y) Rules

- ✅ Use semantic HTML elements (`<button>`, `<nav>`, `<form>`, etc.).
- ✅ All images must include `alt` text.
- ✅ Ensure full keyboard navigability (`Tab`, `Enter`, `Space`, etc.).
- ✅ Follow WCAG AA color contrast (≥ 4.5:1).
- ✅ Use labels with inputs and proper `htmlFor` + `id`.
- ✅ Headings should follow logical order (`h1` → `h2` → `h3`, etc.).
- ✅ Use `eslint-plugin-jsx-a11y`.

---

## 🎨 3. UI/UX Consistency

- ✅ Use shared components from the design system (e.g., buttons, modals).
- ✅ Avoid inline styles; prefer styled-components or utility classes.
- ✅ Maintain consistent spacing with 4pt/8pt grid system.
- ✅ Always handle loading, empty, and error states for async operations.
- ✅ Do not use "magic numbers"; use theme-defined sizes.

---

## 🚀 4. Performance & Code Quality

- ✅ Use `React.memo`, `useCallback`, `useMemo` for optimization.
- ✅ Lazy load large or infrequently used components.
- ✅ Prefer local state (`useState`) over global context unless needed.
- ✅ Avoid anonymous functions in JSX.
- ✅ Type all props (with TypeScript or PropTypes).

---

## 🧪 5. Developer Experience

- ✅ Use ESLint, Prettier, and follow formatting rules.
- ✅ Type everything with TypeScript.
- ✅ One component per file; named exports only.
- ✅ Document props with JSDoc or TS interfaces.
- ✅ All components must include test coverage.

---

## 🧰 Recommended Tooling

- `eslint-config-airbnb` or `eslint-config-next`
- `eslint-plugin-jsx-a11y`
- `eslint-plugin-react-hooks`
- `tailwindcss`
- `prettier`
- `axe-core` (for accessibility testing)
- `jest`, `@testing-library/react`, `playwright` or `cypress`

---

## 🧩 Example: Button Component Cursor Rules

- ✅ Must use semantic `<button>`
- ✅ Must accept `aria-label` when no visible text exists
- ✅ Must support `disabled`, `loading`, and `hover` states
- ✅ Must be responsive (`w-full` on mobile)
- ✅ Must meet WCAG color contrast

---

## ✅ Final Note

Always prefer automation and CLI generators over manual work.
This ensures fewer bugs, consistent structure, and better developer productivity.