import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';

export const appRoutes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard'
  },
  {
    path: 'login',
    loadComponent: () => import('./pages/login/login.component').then((m) => m.LoginComponent)
  },
  {
    path: 'reset_password',
    loadComponent: () =>
      import('./pages/reset-password/reset-password.component').then((m) => m.ResetPasswordComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./pages/dashboard/dashboard.component').then((m) => m.DashboardComponent)
  },
  {
    path: 'tcp_endpoint',
    loadComponent: () =>
      import('./pages/tcp-endpoint/tcp-endpoint.component').then((m) => m.TcpEndpointComponent)
  },
  {
    path: 's3_endpoint',
    loadComponent: () => import('./pages/s3-endpoint/s3-endpoint.component').then((m) => m.S3EndpointComponent)
  },
  {
    path: 'mail_endpoint',
    loadComponent: () =>
      import('./pages/mail-endpoint/mail-endpoint.component').then((m) => m.MailEndpointComponent)
  },
  {
    path: 'manage_users',
    canActivate: [authGuard],
    data: { adminOnly: true },
    loadComponent: () =>
      import('./pages/manage-users/manage-users.component').then((m) => m.ManageUsersComponent)
  },
  {
    path: 'manage_user_kvp_data',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./pages/manage-user-kvp-data/manage-user-kvp-data.component').then(
        (m) => m.ManageUserKvpDataComponent
      )
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/account/account.component').then((m) => m.AccountComponent)
  },
  {
    path: 'http_client',
    loadComponent: () => import('./pages/http-client/http-client.component').then((m) => m.HttpClientComponent)
  },
  {
    path: 'ws_client',
    loadComponent: () => import('./pages/ws-client/ws-client.component').then((m) => m.WsClientComponent)
  },
  {
    path: 'live_feed',
    loadComponent: () => import('./pages/live-feed/live-feed.component').then((m) => m.LiveFeedComponent)
  },
  {
    path: '**',
    loadComponent: () => import('./pages/not-found/not-found.component').then((m) => m.NotFoundComponent)
  }
];
