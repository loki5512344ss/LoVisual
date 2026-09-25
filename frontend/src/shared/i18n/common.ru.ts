export const commonRu = {
  skipToContent: 'Перейти к содержимому',
  nav: {
    mainNav: 'Основная навигация',
    showcase: 'Витрина',
    myConfigs: 'Мои конфиги',
    linkGame: 'Привязать игру',
    menu: 'Меню',
    login: 'Войти',
  },
  notFound: {
    title: 'Такой страницы нет',
    body: 'Возможно, ссылка устарела.',
    home: 'На главную',
  },
  shareCode: {
    copy: 'Скопировать',
    copied: 'Скопировано',
  },
  languageSwitch: {
    ru: 'RU',
    en: 'EN',
  },
  footer: {
    about: 'Бесплатный визуальный мод для Minecraft 26.2 на Fabric.',
    sectionProduct: 'Продукт',
    sectionProject: 'Проект',
    github: 'GitHub',
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
