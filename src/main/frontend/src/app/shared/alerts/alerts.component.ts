import { Component, Input } from '@angular/core';
import { NgClass, NgFor } from '@angular/common';

import { Alert } from '../../core/models';
import { I18nPipe } from '../../core/i18n.pipe';

@Component({
  selector: 'app-alerts',
  standalone: true,
  imports: [I18nPipe, NgFor, NgClass],
  templateUrl: './alerts.component.html',
  styleUrls: ['./alerts.component.css']
})
export class AlertsComponent {
  @Input() alerts: Alert[] = [];

  dismiss(index: number): void {
    this.alerts.splice(index, 1);
  }
}
