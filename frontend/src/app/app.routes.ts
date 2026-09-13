import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./pages/login/login').then((m) => m.Login) },
  {
    path: 'calls',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/call-list/call-list').then((m) => m.CallList),
  },
  {
    path: 'calls/:callId',
    canActivate: [authGuard],
    loadComponent: () => import('./pages/call-detail/call-detail').then((m) => m.CallDetailPage),
  },
  { path: '', pathMatch: 'full', redirectTo: 'calls' },
  { path: '**', redirectTo: 'calls' },
];
