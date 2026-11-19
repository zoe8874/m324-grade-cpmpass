import { Routes } from '@angular/router';
import { Dashboard } from './dashboard/dashboard';
import { Grades } from './grades/grades';
import { Subjects } from './subjects/subjects';

export const routes: Routes = [
  { path: '', pathMatch: 'full', component: Dashboard },
  { path: 'subjects', component: Subjects },
  { path: 'grades', component: Grades },
  { path: '**', redirectTo: '' },
];
