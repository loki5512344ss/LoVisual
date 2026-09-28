import type { ButtonHTMLAttributes } from 'react'
import { Link, type LinkProps } from 'react-router'

type Variant = 'primary' | 'quiet' | 'danger'

const BASE =
  'inline-flex items-center justify-center gap-2 rounded-ctl px-4 py-2 text-sm font-semibold transition-colors'

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
      className={`${BASE} disabled:cursor-not-allowed disabled:opacity-60 ${VARIANTS[variant]} ${className}`}
      {...rest}
    />
  )
}

/** A router link that looks like a button — for navigation CTAs. */
export function ButtonLink({ variant = 'primary', className = '', ...rest }: LinkProps & { variant?: Variant }) {
  return <Link className={`${BASE} ${VARIANTS[variant]} ${className}`} {...rest} />
}
