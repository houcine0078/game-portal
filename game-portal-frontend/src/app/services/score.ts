import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class ScoreService {
  private baseUrl = 'http://localhost:8080/api/scores';

  constructor(private http: HttpClient) {}

  saveScore(data: { username: string; gameType: string; score: number; accuracy?: number; duration?: number }): Observable<any> {
    return this.http.post(this.baseUrl, data, { responseType: 'text' });
  }

  getLeaderboard(gameType: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/leaderboard?game=${gameType}`);
  }

  getHistory(): Observable<any[]> {
    return this.http.get<any[]>(`${this.baseUrl}/history`);
  }
}
