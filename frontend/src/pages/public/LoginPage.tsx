import { LoginForm } from '../../features/auth/forms/LoginForm'

export default function LoginPage() {
  return (
    <section className="flex flex-col gap-6 py-12">
      <h1 className="text-3xl">Вход</h1>
      <LoginForm />
    </section>
  )
}
