/**
 * True when the visitor asked for reduced motion. Unknown support (no matchMedia, e.g. jsdom)
 * counts as reduced: the safe default, and it keeps tests deterministic.
 */
export function prefersReducedMotion(): boolean {
  return typeof window.matchMedia !== 'function' || window.matchMedia('(prefers-reduced-motion: reduce)').matches
}
