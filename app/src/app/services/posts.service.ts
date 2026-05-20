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
  caption?: string;
  name?: string;
  mobile?: string;
  area?: string;
  cost?: string;
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

  addVideoPost(params: {
    objectUrl: string;
    durationSec: number;
    caption?: string;
    name?: string;
    mobile?: string;
    area?: string;
    cost?: string;
  }) {
    const post: PostVideo = {
      id: crypto.randomUUID(),
      createdAt: Date.now(),
      durationSec: params.durationSec,
      objectUrl: params.objectUrl,
      caption: params.caption,
      name: params.name,
      mobile: params.mobile,
      area: params.area,
      cost: params.cost,
    };
    this._posts$.next([post, ...this._posts$.value]);
    return post;
  }

  updatePostDetails(
    postId: string,
    details: { name?: string; mobile?: string; area?: string; cost?: string }
  ) {
    const next = this._posts$.value.map((p) => (p.id === postId ? { ...p, ...details } : p));
    this._posts$.next(next);
  }

  setContact(postId: string, contact: { name: string; mobile: string; place: string }) {
    const next = this._posts$.value.map((p) => (p.id === postId ? { ...p, contact } : p));
    this._posts$.next(next);
  }
}

