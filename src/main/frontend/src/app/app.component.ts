import { Component, Inject } from '@angular/core';
import { AsyncPipe, DOCUMENT, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';
import { I18nPipe } from './core/i18n.pipe';
import { I18nService, LanguageCode, LanguageOption } from './core/i18n.service';

interface NavLink {
  labelKey: string;
  path: string;
}

type ThemeMode = 'light' | 'dark';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [AsyncPipe, FormsModule, I18nPipe, NgFor, NgIf, RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './app.component.html',
  styleUrls: ['./app.component.css']
})
export class AppComponent {
  title = 'sMockin';
  theme: ThemeMode = 'light';
  private readonly themeStorageKey = 'smockin-theme';
  navLinks: NavLink[] = [
    { labelKey: 'nav.dashboard', path: '/dashboard' },
    { labelKey: 'nav.http', path: '/tcp_endpoint' },
    { labelKey: 'nav.s3', path: '/s3_endpoint' },
    { labelKey: 'nav.mail', path: '/mail_endpoint' }
  ];

  toolLinks: NavLink[] = [
    { labelKey: 'nav.httpClient', path: '/http_client' },
    { labelKey: 'nav.wsClient', path: '/ws_client' },
    { labelKey: 'nav.liveFeed', path: '/live_feed' }
  ];

  accountLink: NavLink = { labelKey: 'nav.account', path: '/account' };

  adminLinks: NavLink[] = [
    { labelKey: 'nav.users', path: '/manage_users' }
  ];

  constructor(
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly i18n: I18nService,
    @Inject(DOCUMENT) private readonly document: Document
  ) {
    this.theme = this.resolveInitialTheme();
    this.applyTheme(this.theme);
  }

  get authState$() {
    return this.auth.state$;
  }

  get languages(): LanguageOption[] {
    return this.i18n.availableLanguages;
  }

  get language(): LanguageCode {
    return this.i18n.currentLanguage;
  }

  set language(code: LanguageCode) {
    this.i18n.use(code);
  }

  logout(): void {
    this.auth.logout();
    this.router.navigate(['/login']);
  }

  closeToolsMenu(event: FocusEvent): void {
    const menu = event.currentTarget as HTMLDetailsElement | null;
    if (!menu) {
      return;
    }

    const nextTarget = event.relatedTarget as Node | null;
    if (!nextTarget || !menu.contains(nextTarget)) {
      menu.open = false;
    }
  }

  onThemeChange(theme: ThemeMode): void {
    this.theme = theme;
    localStorage.setItem(this.themeStorageKey, theme);
    this.applyTheme(theme);
  }

  isAdmin(): boolean {
    return this.auth.isAdmin();
  }

  isLoggedIn(): boolean {
    return this.auth.isLoggedIn();
  }

  private resolveInitialTheme(): ThemeMode {
    const stored = localStorage.getItem(this.themeStorageKey);
    if (stored === 'light' || stored === 'dark') {
      return stored;
    }

    if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
      return 'dark';
    }

    return 'light';
  }

  private applyTheme(theme: ThemeMode): void {
    this.document.documentElement.setAttribute('data-theme', theme);
  }
}
