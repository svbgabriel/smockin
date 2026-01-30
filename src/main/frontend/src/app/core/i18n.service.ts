import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, firstValueFrom } from 'rxjs';

export type LanguageCode = 'en' | 'pt';

export interface LanguageOption {
  code: LanguageCode;
  label: string;
}

type TranslationMap = Record<string, string>;

@Injectable({ providedIn: 'root' })
export class I18nService {
  private readonly storageKey = 'smockin.language';
  private readonly translations = new Map<LanguageCode, TranslationMap>();
  private readonly languageSubject = new BehaviorSubject<LanguageCode>(this.getInitialLanguage());

  readonly language$ = this.languageSubject.asObservable();

  constructor(private readonly http: HttpClient) {}

  get currentLanguage(): LanguageCode {
    return this.languageSubject.value;
  }

  get availableLanguages(): LanguageOption[] {
    return [
      { code: 'en', label: 'English' },
      { code: 'pt', label: 'Português' }
    ];
  }

  async init(): Promise<void> {
    await this.use(this.currentLanguage);
  }

  async use(language: LanguageCode): Promise<void> {
    if (!this.translations.has(language)) {
      const payload = await firstValueFrom(this.http.get<TranslationMap>(`assets/i18n/${language}.json`));
      this.translations.set(language, payload ?? {});
    }

    this.languageSubject.next(language);
    if (typeof document !== 'undefined') {
      document.documentElement.lang = language;
    }
    localStorage.setItem(this.storageKey, language);
  }

  t(key: string, params?: Record<string, string | number>): string {
    if (!key) {
      return '';
    }

    const dictionary = this.translations.get(this.currentLanguage) ?? {};
    const template = dictionary[key] ?? key;
    if (!params) {
      return template;
    }

    return template.replace(/\{\{([\w.-]+)\}\}/g, (match, token) => {
      const value = params[token];
      return value === undefined ? match : String(value);
    });
  }

  private getInitialLanguage(): LanguageCode {
    const stored = localStorage.getItem(this.storageKey);
    if (stored === 'en' || stored === 'pt') {
      return stored;
    }

    const browser = navigator.language.toLowerCase();
    if (browser.startsWith('pt')) {
      return 'pt';
    }
    return 'en';
  }
}
