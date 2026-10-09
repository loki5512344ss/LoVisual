export const commonRu = {
  skipToContent: 'Перейти к содержимому',
  konami: '+30 жизней. Радуга включена.',
  nav: {
    mainNav: 'Основная навигация',
    home: 'LoVisual · на главную',
    showcase: 'Витрина',
    themes: 'Темы',
    myConfigs: 'Мои конфиги',
    linkGame: 'Привязать игру',
    menu: 'Меню',
    login: 'Войти',
    account: 'Аккаунт',
    sessionLoading: 'Проверяем вход',
  },
  notFound: {
    title: 'Такой страницы нет',
    body: 'Возможно, ссылка устарела.',
    home: 'На главную',
    console: '> chunk not found. Попробуй ещё раз.',
    blockAria: 'Сломать блок',
    blockHint: 'Кликни по блоку — он треснет и рассыплется, как в игре.',
  },
  shareCode: {
    copy: 'Скопировать',
    copied: 'Скопировано',
  },
  languageSwitch: {
    label: 'Язык',
    ru: 'RU',
    en: 'EN',
  },
  footer: {
    about: 'Бесплатный визуальный мод для Minecraft 26.2 на Fabric.',
    sectionProduct: 'Продукт',
    sectionProject: 'Проект',
    github: 'Исходный код',
    download: 'Скачать',
    legalTitle: 'Правовое',
    disclaimer: 'Не связано с Mojang или Microsoft.',
  },
  time: {
    days_one: '{{count}} день',
    days_few: '{{count}} дня',
    days_many: '{{count}} дней',
    days_other: '{{count}} дня',
  },
  errors: {
    badRequest: 'Проверь введённые данные.',
    unauthorized: 'Нужно войти в аккаунт.',
    forbidden: 'Недостаточно прав.',
    notFound: 'Не найдено.',
    conflict: 'Уже существует.',
    rateLimited: 'Слишком много попыток. Подожди {{seconds}} с.',
    rateLimitedGeneric: 'Слишком много попыток. Подожди немного.',
    serverError: 'Сервер не ответил. Попробуй через минуту.',
    network: 'Нет связи с сервером. Проверь интернет и попробуй ещё раз.',
  },
} as const

export type Translation<T> = {
  [K in keyof T]: T[K] extends string ? string : Translation<T[K]>
}
