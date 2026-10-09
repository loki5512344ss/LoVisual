import type { Translation } from '@shared/i18n/common.ru'
import type { authRu } from './ru'

export const authEn = {
  checkingSession: 'Checking session…',
  tabs: {
    label: 'Log in or sign up',
    login: 'Log in',
    register: 'Sign up',
  },
  hintsLabel: 'Hints',
  next: {
    link: 'Next: you confirm the code from the game.',
    configs: 'Next: your cloud slots.',
    account: 'Next: your account settings.',
    showcase: 'Next: the showcase config goes into your slot.',
  },
  login: {
    title: 'Welcome back',
    emailLabel: 'Email',
    passwordLabel: 'Password',
    submit: 'Log in',
    wrongCredentials: 'Wrong email or password.',
    hints: {
      one: 'No account - the "Sign up" tab up top',
      two: 'The password is for the site only',
      three: 'The mod signs in by itself, with a code from its Account tab',
    },
  },
  register: {
    title: 'New account',
    emailLabel: 'Email',
    nickLabel: 'Site nickname',
    nickHint: "Doesn't have to match your Minecraft nick.",
    passwordLabel: 'Password',
    passwordHint: 'At least 8 characters.',
    submit: 'Create account',
    emailTaken: 'That email is already taken. Log in or use another one.',
    hints: {
      one: 'Account - 4 cloud slots for your configs',
      two: 'Slot code - send it to a friend or load it on another PC',
      three: 'No password in the mod, it signs in with a code',
      four: 'The mod works without an account too',
    },
  },
  validation: {
    emailRequired: 'Enter your email.',
    emailInvalid: 'That email looks off.',
    passwordShort: 'At least 8 characters.',
    passwordTooLong: 'Password is too long.',
    nickRequired: 'Pick a nickname.',
    nickTooLong: 'No longer than 32 characters.',
  },
  banner: {
    text: 'Confirm your email to link the mod to the account. The message went to {{email}}.',
    resend: 'Send the email again',
    resent: 'Email sent. Check the spam folder if nothing arrived.',
    failed: 'Could not send the email. Try again later.',
  },
  forgot: {
    title: 'Password recovery',
    emailLabel: 'Email',
    submit: 'Send the email',
    sent: 'If that address is registered, the reset email is on its way.',
    backToLogin: 'Back to log in',
  },
  reset: {
    title: 'New password',
    passwordLabel: 'New password',
    passwordHint: 'At least 8 characters.',
    submit: 'Change password',
    done: 'Password updated. Log in with the new one.',
    badToken: 'The link is invalid or expired. Request a new reset.',
    goLogin: 'To log in',
  },
  verify: {
    title: 'Email verification',
    checking: 'Verifying…',
    done: 'Email verified. The mod can now be linked on the Account tab.',
    badToken: 'The link is invalid or expired.',
    goAccount: 'To the Account tab',
  },
} satisfies Translation<typeof authRu>
