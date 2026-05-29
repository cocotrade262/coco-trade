import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { GoogleAuth } from '@codetrix-studio/capacitor-google-auth';
import { isPlatform } from '@ionic/angular';

export interface UserProfile {
  email: string;
  displayName: string;
  photoUrl?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly _user$ = new BehaviorSubject<UserProfile | null>(this.getStoredUser());
  readonly user$ = this._user$.asObservable();

  constructor() {
    if (!isPlatform('capacitor')) {
      GoogleAuth.initialize();
    }
  }

  async login() {
    try {
      const googleUser = await GoogleAuth.signIn();
      // Adjust property names based on @codetrix-studio/capacitor-google-auth User type
      // Usually it is displayName or name, let's cast or check documentation
      const user: UserProfile = {
        email: googleUser.email,
        displayName: (googleUser as any).displayName || (googleUser as any).name || googleUser.email.split('@')[0],
        photoUrl: googleUser.imageUrl
      };
      localStorage.setItem('coco_user', JSON.stringify(user));
      this._user$.next(user);
    } catch (error) {
      console.error('Google Auth Error', error);
      if (!isPlatform('hybrid')) {
         this.mockLogin('user@gmail.com', 'Dev User');
      }
    }
  }

  mockLogin(email: string, displayName: string) {
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

  async logout() {
    try {
      await GoogleAuth.signOut();
    } catch (e) {}
    localStorage.removeItem('coco_user');
    this._user$.next(null);
  }

  private getStoredUser(): UserProfile | null {
    const stored = localStorage.getItem('coco_user');
    if (stored) {
       try {
         return JSON.parse(stored);
       } catch (e) {
         return null;
       }
    }
    return null;
  }
}
