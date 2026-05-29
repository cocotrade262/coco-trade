import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { isPlatform } from '@ionic/angular';

export interface UserProfile {
  uid: string;
  email: string;
  displayName: string;
  photoUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  // Use a new storage key to invalidate all legacy mock sessions
  private static readonly AUTH_KEY = 'cocotrade_v1_auth';

  private readonly _user$ = new BehaviorSubject<UserProfile | null>(this.getStoredUser());
  readonly user$ = this._user$.asObservable();

  constructor() {
    this.initialize();
  }

  private async initialize() {
    if (!isPlatform('capacitor')) {
      GoogleAuth.initialize({
        clientId: '274853330536-vlt0nphh115t707f1o90q9l56asolp3q.apps.googleusercontent.com',
        scopes: ['profile', 'email'],
        grantOfflineAccess: true,
      });
    }

    // Attempt to refresh/verify existing session on load
    try {
      const user = await GoogleAuth.refresh();
      if (user) {
        this.updateUserState(user);
      } else {
        // If refresh fails or returns null, ensure local state is cleared
        this.clearUserState();
      }
    } catch (e) {
      console.log('No active session found on load');
      // If we had a stored user but refresh failed, we might want to clear it
      // to avoid the "mock user" appearance if it was stale.
      this.clearUserState();
    }
  }

  async login() {
    try {
      const googleUser = await GoogleAuth.signIn();
      this.updateUserState(googleUser);
    } catch (error) {
      console.error('Google Auth Error', error);
    }
  }

  private updateUserState(googleUser: any) {
    const user: UserProfile = {
      uid: googleUser.id || googleUser.uid,
      email: googleUser.email,
      displayName: googleUser.displayName || googleUser.name || googleUser.email.split('@')[0],
      photoUrl: googleUser.imageUrl
    };
    localStorage.setItem(AuthService.AUTH_KEY, JSON.stringify(user));
    this._user$.next(user);
  }

  private clearUserState() {
    localStorage.removeItem(AuthService.AUTH_KEY);
    // Also remove the old key if it exists
    localStorage.removeItem('coco_user');
    this._user$.next(null);
  }

  updateDisplayName(name: string) {
    const current = this._user$.value;
    if (current) {
      const updated = { ...current, displayName: name };
      localStorage.setItem(AuthService.AUTH_KEY, JSON.stringify(updated));
      this._user$.next(updated);
    }
  }

  async logout() {
    try {
      await GoogleAuth.signOut();
    } catch (e) {}
    this.clearUserState();
  }

  private getStoredUser(): UserProfile | null {
    const stored = localStorage.getItem(AuthService.AUTH_KEY);
    if (stored) {
       try {
         const user = JSON.parse(stored);
         // Basic validation: ensure it's not the old mock user
         if (user.email === 'user@gmail.com' && user.uid === 'mock_uid_123') {
           return null;
         }
         return user;
       } catch (e) {
         return null;
       }
    }
    return null;
  }
}
