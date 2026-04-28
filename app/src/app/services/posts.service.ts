import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

export type PostVideo = {
  id: string;
  createdAt: number;
  durationSec: number;
  /**
   * In-memory object URL. Not persisted across reloads.
   */
  objectUrl: string;
  likes: number;
  commentsCount: number;
  contact?: {
    name: string;
    mobile: string;
    place: string;
  };
};

@Injectable({ providedIn: 'root' })
export class PostsService {
  private readonly _posts$ = new BehaviorSubject<PostVideo[]>([]);
  readonly posts$ = this._posts$.asObservable();

  addVideoPost(params: { objectUrl: string; durationSec: number }) {
    const post: PostVideo = {
      id: crypto.randomUUID(),
      createdAt: Date.now(),
      durationSec: params.durationSec,
      objectUrl: params.objectUrl,
      likes: 0,
      commentsCount: 0,
    };
    this._posts$.next([post, ...this._posts$.value]);
    return post;
  }

  toggleLike(postId: string) {
    const next = this._posts$.value.map((p) =>
      p.id === postId ? { ...p, likes: Math.max(0, p.likes + 1) } : p
    );
    this._posts$.next(next);
  }

  setContact(postId: string, contact: { name: string; mobile: string; place: string }) {
    const next = this._posts$.value.map((p) => (p.id === postId ? { ...p, contact } : p));
    this._posts$.next(next);
  }
}

