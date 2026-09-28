import { useId, type InputHTMLAttributes } from 'react'

export function TextField({
  label,
  error,
  hint,
  className = '',
  ...rest
}: InputHTMLAttributes<HTMLInputElement> & { label: string; error?: string; hint?: string }) {
  const id = useId()
  const describedBy = error ? `${id}-error` : hint ? `${id}-hint` : undefined
  return (
    <div className={`flex flex-col gap-1.5 ${className}`}>
      <label htmlFor={id} className="text-sm text-frost">
        {label}
      </label>
      <input
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy}
        className="rounded-ctl border border-shoal bg-abyss px-3 py-2 text-snow placeholder:text-frost/60 focus:border-ice aria-invalid:border-ember"
        {...rest}
      />
      {error ? (
        <p id={`${id}-error`} className="text-sm text-ember">
          {error}
        </p>
      ) : hint ? (
        <p id={`${id}-hint`} className="text-sm text-frost">
          {hint}
        </p>
      ) : null}
    </div>
  )
}
