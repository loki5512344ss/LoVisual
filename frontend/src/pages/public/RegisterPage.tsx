import { RegisterForm } from '../../features/auth/forms/RegisterForm'

export default function RegisterPage() {
  return (
    <section className="flex flex-col gap-6 py-12">
      <h1 className="text-3xl">Новый аккаунт</h1>
      <p className="max-w-sm text-frost">
        Аккаунт нужен для облачных конфигов и привязки игры. Пароль в мод вводить не придётся.
      </p>
      <RegisterForm />
    </section>
  )
}
