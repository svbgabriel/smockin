import { Component, OnInit } from '@angular/core';
import { NgFor, NgIf } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

import { ApiService } from '../../core/api.service';
import { AlertsComponent } from '../../shared/alerts/alerts.component';
import { Alert, KvpResponse, SimpleMessageResponse } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';
import { I18nService } from '../../core/i18n.service';

@Component({
  selector: 'app-manage-user-kvp-data',
  standalone: true,
  imports: [AlertsComponent, I18nPipe, NgFor, NgIf, ReactiveFormsModule],
  templateUrl: './manage-user-kvp-data.component.html',
  styleUrls: ['./manage-user-kvp-data.component.css']
})
export class ManageUserKvpDataComponent implements OnInit {
  alerts: Alert[] = [];
  kvpData: KvpResponse[] = [];
  selectedKvp: KvpResponse | null = null;
  entryMode: 'single' | 'bulk' = 'single';
  isBusy = false;

  form!: FormGroup;

  constructor(private readonly api: ApiService, private readonly fb: FormBuilder, private readonly i18n: I18nService) {
    this.form = this.fb.group({
      key: ['', Validators.required],
      value: ['', Validators.required],
      bulkValue: ['']
    });
  }

  ngOnInit(): void {
    this.loadKvp();
  }

  startNew(): void {
    this.selectedKvp = null;
    this.entryMode = 'single';
    this.form.reset();
  }

  selectKvp(item: KvpResponse): void {
    this.selectedKvp = item;
    this.entryMode = 'single';
    this.form.reset({
      key: item.key,
      value: item.value,
      bulkValue: ''
    });
  }

  save(): void {
    if (this.entryMode === 'bulk' && !this.selectedKvp) {
      this.saveBulk();
      return;
    }

    const keyRaw = this.form.controls['key'].value ?? '';
    const valueRaw = this.form.controls['value'].value ?? '';
    const key = `${keyRaw}`.trim();
    const value = `${valueRaw}`.trim();

    if (!key || !value) {
      this.showAlert(this.i18n.t('manageKvp.errors.requiredFields'));
      return;
    }

    if (!this.selectedKvp) {
      this.isBusy = true;
      this.api
        .post<SimpleMessageResponse>('/keyvaluedata', [{ extId: null, key, value }])
        .subscribe({
          next: () => {
            this.isBusy = false;
            this.showAlert(this.i18n.t('manageKvp.success.saved'), 'success');
            this.loadKvp();
            this.startNew();
          },
          error: (err) => {
            this.isBusy = false;
            if (err?.status === 409) {
              this.showAlert(this.i18n.t('manageKvp.errors.keyExists', { key }));
              return;
            }
            if (err?.status === 400) {
              this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
              return;
            }
            this.showAlert(this.i18n.t('common.errors.generic'));
          }
        });
      return;
    }

    this.isBusy = true;
    this.api
      .put<void>(`/keyvaluedata/${this.selectedKvp.extId}`, {
        extId: this.selectedKvp.extId,
        key,
        value
      })
      .subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('manageKvp.success.updated'), 'success');
        this.loadKvp();
      },
      error: (err) => {
        this.isBusy = false;
        if (err?.status === 400) {
          this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  saveBulk(): void {
    const bulkValue = this.form.controls['bulkValue'].value ?? '';
    const trimmedBulkValue = `${bulkValue}`.trim();
    if (!trimmedBulkValue || (!bulkValue.includes('\n') && !bulkValue.includes('='))) {
      this.showAlert(this.i18n.t('manageKvp.errors.bulkRequired'));
      return;
    }

    const lines = `${bulkValue}`.split('\n');
    const payload: Array<{ extId: null; key: string; value: string }> = [];

    for (const line of lines) {
      const parts = line.split('=');
      if (parts.length !== 2) {
        this.showAlert(this.i18n.t('manageKvp.errors.bulkInvalidEntry'));
        return;
      }
      const key = parts[0].trim();
      const value = parts[1].trim();
      if (!key || !value) {
        this.showAlert(this.i18n.t('manageKvp.errors.bulkInvalidEntry'));
        return;
      }
      payload.push({ extId: null, key, value });
    }

    this.isBusy = true;
    this.api.post<SimpleMessageResponse>('/keyvaluedata', payload).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('manageKvp.success.bulkSaved'), 'success');
        this.loadKvp();
        this.startNew();
      },
      error: (err) => {
        this.isBusy = false;
        if (err?.status === 409) {
          this.showAlert(this.i18n.t('manageKvp.errors.bulkKeyExists'));
          return;
        }
        if (err?.status === 400) {
          this.showAlert(err.error?.message ?? this.i18n.t('common.errors.invalidData'));
          return;
        }
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  delete(): void {
    if (!this.selectedKvp) {
      return;
    }
    if (!confirm(this.i18n.t('manageKvp.confirm.delete'))) {
      return;
    }

    this.isBusy = true;
    this.api.delete<void>(`/keyvaluedata/${this.selectedKvp.extId}`).subscribe({
      next: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('manageKvp.success.deleted'), 'success');
        this.loadKvp();
        this.startNew();
      },
      error: () => {
        this.isBusy = false;
        this.showAlert(this.i18n.t('common.errors.generic'));
      }
    });
  }

  private loadKvp(): void {
    this.api.get<KvpResponse[]>('/keyvaluedata').subscribe({
      next: (data) => {
        this.kvpData = data ?? [];
      },
      error: () => {
        this.showAlert(this.i18n.t('manageKvp.errors.loadData'));
      }
    });
  }

  private showAlert(message: string, type: Alert['type'] = 'danger'): void {
    this.alerts = [{ message, type }];
  }
}
