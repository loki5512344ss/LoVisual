import type { Translation } from '../../../shared/i18n/common.ru'
import type { authRu } from './ru'

export const authEn = {
  checkingSession: 'Checking session…',
  login: {
    title: 'Log in',
    emailLabel: 'Email',
    passwordLabel: 'Password',
    submit: 'Log in',
    noAccount: "Don't have an account?",
    createAccount: 'Sign up',
    wrongCredentials: 'Wrong email or password.',
  },
  register: {
    title: 'New account',
    subtitle: "You'll need an account for cloud configs and linking the game. No password required in the mod.",
    emailLabel: 'Email',
    nickLabel: 'Site nickname',
    nickHint: "Doesn't have to match your Minecraft nick.",
    passwordLabel: 'Password',
    passwordHint: 'At least 8 characters.',
    submit: 'Create account',
    haveAccount: 'Already have an account?',
    login: 'Log in',
    emailTaken: 'That email is already taken. Log in or use another one.',
  },
  validation: {
    emailRequired: 'Enter your email.',
    emailInvalid: 'That email looks off.',
    passwordShort: 'At least 8 characters.',
    passwordTooLong: 'Password is too long.',
    nickRequired: 'Pick a nickname.',
    nickTooLong: 'No longer than 32 characters.',
  },
} satisfies Translation<typeof authRu>
