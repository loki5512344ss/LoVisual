import type { ButtonHTMLAttributes } from 'react'

type Variant = 'primary' | 'quiet' | 'danger'

const VARIANTS: Record<Variant, string> = {
  primary: 'bg-ice text-abyss hover:bg-ice/90',
  quiet: 'bg-transparent text-snow border border-shoal hover:border-frost',
  danger: 'bg-transparent text-ember border border-ember/60 hover:border-ember',
}

export function Button({
  variant = 'primary',
  busy = false,
  disabled,
  className = '',
  type = 'button',
  ...rest
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; busy?: boolean }) {
  return (
    <button
      type={type}
      disabled={disabled || busy}
      aria-busy={busy || undefined}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-4 py-2 text-sm font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${className}`}
      {...rest}
    />
  )
}
