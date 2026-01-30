import { Component } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute } from '@angular/router';
import { NgIf } from '@angular/common';

import { AuthService } from '../../core/auth.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [AlertsComponent, I18nPipe, NgIf, ReactiveFormsModule],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent {
  alerts: Alert[] = [];
  isSubmitting = false;

  form!: FormGroup;

  constructor(
    private readonly fb: FormBuilder,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly route: ActivatedRoute,
    private readonly i18n: I18nService
  ) {
    this.form = this.fb.group({
      username: ['', Validators.required],
      password: ['', Validators.required]
    });
  }

  submit(): void {
    if (this.form.invalid) {
      this.showAlert(this.i18n.t('login.errors.missingCredentials'));
      return;
    }

    const { username, password } = this.form.getRawValue();
    if (!username || !password) {
      this.showAlert(this.i18n.t('login.errors.missingCredentials'));
      return;
    }

    this.isSubmitting = true;

    this.auth.login(username, password).subscribe({
      next: () => {
        const redirect = this.route.snapshot.queryParamMap.get('redirect') || '/dashboard';
        this.router.navigateByUrl(redirect);
      },
      error: (err) => {
        if (err?.status === 401) {
          this.showAlert(this.i18n.t('login.errors.invalid'));
        } else {
          this.showAlert(this.i18n.t('login.errors.failed'));
        }
        this.isSubmitting = false;
      }
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
