import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export interface UserProfile {
  email: string;
  displayName: string;
  photoUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user$ = new BehaviorSubject<UserProfile | null>(this.getStoredUser());
  readonly user$ = this._user$.asObservable();

  login(email: string, displayName: string) {
    const user: UserProfile = { email, displayName };
    localStorage.setItem('coco_user', JSON.stringify(user));
    this._user$.next(user);
  }

  updateDisplayName(name: string) {
    const current = this._user$.value;
    if (current) {
      const updated = { ...current, displayName: name };
      localStorage.setItem('coco_user', JSON.stringify(updated));
      this._user$.next(updated);
    }
  }

  logout() {
    localStorage.removeItem('coco_user');
    this._user$.next(null);
  }

  private getStoredUser(): UserProfile | null {
    const stored = localStorage.getItem('coco_user');
    return stored ? JSON.parse(stored) : null;
  }
}
