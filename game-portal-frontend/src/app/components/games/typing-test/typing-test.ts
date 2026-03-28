import { Component, OnInit, OnDestroy, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';

interface TypingWord {
  target: string;
  typed: string;
}

type GameStatus = 'waiting' | 'playing' | 'finished';

@Component({
  selector: 'app-typing-test',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './typing-test.html',
  styleUrls: ['./typing-test.css'],
})
export class TypingTestComponent implements OnInit, OnDestroy {
  readonly Math = Math;

  readonly DICTIONARY: string[] = [
    'the','be','to','of','and','a','in','that','have','it','for','not','on','with',
    'he','as','you','do','at','this','but','his','by','from','they','we','say','her',
    'she','or','an','will','my','one','all','would','there','their','what','so','up',
    'out','if','about','who','get','which','go','me','when','make','can','like','time',
    'no','just','him','know','take','people','into','year','your','good','some','could',
    'them','see','other','than','then','now','look','only','come','its','over','think',
    'also','back','after','use','two','how','our','work','first','well','way','even',
    'new','want','because','any','these','give','day','most','us','great','between',
    'need','large','often','hand','high','place','hold','turn','without','last','never',
    'own','while','both','life','few','north','open','seem','together','next','white',
    'children','begin','got','walk','example','ease','paper','group','always','music',
    'those','mark','book','carry','took','science','eat','room','friend','began','idea',
  ];

  readonly TIME_OPTIONS: number[] = [30, 60, 120];

  // --- Word state ---
  words: TypingWord[] = [];
  wordIndex = 0;

  // --- Game state ---
  status: GameStatus = 'waiting';
  selectedTime = 30;
  timeLeft = 30;
  private timerInterval: ReturnType<typeof setInterval> | null = null;

  // --- Stats ---
  wpm = 0;
  accuracy = 0;
  correctWords = 0;
  correctChars = 0;

  // --- Live tracking ---
  liveWpm = 0;
  liveAccuracy: string = '—';
  progress = 0;

  // --- WPM history for chart ---
  wpmHistory: number[] = [];
  private secondsElapsed = 0;
  private correctCharsTotal = 0;

  // ─── Lifecycle ────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.generateWords();
  }

  ngOnDestroy(): void {
    this.clearTimer();
  }

  // ─── Word generation ──────────────────────────────────────────────────────

  generateWords(): void {
    this.words = Array.from({ length: 120 }, () => ({
      target: this.DICTIONARY[Math.floor(Math.random() * this.DICTIONARY.length)],
      typed: '',
    }));
  }

  // ─── Keyboard handler ─────────────────────────────────────────────────────

  @HostListener('window:keydown', ['$event'])
  handleKeyDown(event: KeyboardEvent): void {
    if (event.key === 'Tab') {
      event.preventDefault();
      this.restartGame();
      return;
    }

    if (this.status === 'finished') return;

    // Begin on first real keystroke
    if (this.status === 'waiting' && event.key.length === 1 && event.key !== ' ') {
      this.startGame();
    }

    const current = this.words[this.wordIndex];
    if (!current) return;

    if (event.key === 'Backspace') {
      if (current.typed.length > 0) {
        current.typed = current.typed.slice(0, -1);
      } else if (this.wordIndex > 0) {
        this.wordIndex--;
      }
    } else if (event.key === ' ') {
      event.preventDefault();
      if (current.typed.length > 0) {
        this.wordIndex++;
        if (this.wordIndex >= this.words.length) {
          this.endGame();
          return;
        }
        this.scrollToCurrentWord();
      }
    } else if (event.key.length === 1) {
      if (current.typed.length < current.target.length + 5) {
        current.typed += event.key;
      }
    }

    this.updateLiveStats();
  }

  // ─── Game flow ────────────────────────────────────────────────────────────

  startGame(): void {
    this.status = 'playing';
    this.wpmHistory = [];
    this.secondsElapsed = 0;
    this.correctCharsTotal = 0;

    this.timerInterval = setInterval(() => {
      this.timeLeft--;
      this.secondsElapsed++;

      // Snapshot WPM each second for chart
      const elapsed = this.secondsElapsed / 60;
      const snapshot = Math.round((this.correctCharsTotal / 5) / elapsed);
      this.wpmHistory.push(snapshot);

      if (this.timeLeft <= 0) this.endGame();
    }, 1000);
  }

  endGame(): void {
    this.status = 'finished';
    this.clearTimer();
    this.calculateStats();
  }

  calculateStats(): void {
    let totalCorrect = 0;
    let totalTyped = 0;
    let words = 0;

    for (let i = 0; i <= this.wordIndex; i++) {
      const w = this.words[i];
      if (!w.typed) continue;

      totalTyped += w.typed.length;

      for (let j = 0; j < w.typed.length; j++) {
        if (w.typed[j] === w.target[j]) totalCorrect++;
      }

      if (i < this.wordIndex && w.typed === w.target) {
        totalCorrect++;  // space counted as correct
        totalTyped++;
        words++;
      }
    }

    const minutes = this.selectedTime / 60;
    this.wpm = Math.round((totalCorrect / 5) / minutes);
    this.accuracy = totalTyped === 0 ? 0 : Math.round((totalCorrect / totalTyped) * 100);
    this.correctWords = words;
    this.correctChars = totalCorrect;
  }

  setTime(seconds: number): void {
    this.selectedTime = seconds;
    this.restartGame();
  }

  restartGame(): void {
    this.clearTimer();
    this.status = 'waiting';
    this.timeLeft = this.selectedTime;
    this.wordIndex = 0;
    this.wpm = 0;
    this.accuracy = 0;
    this.correctWords = 0;
    this.correctChars = 0;
    this.liveWpm = 0;
    this.liveAccuracy = '—';
    this.progress = 0;
    this.wpmHistory = [];
    this.secondsElapsed = 0;
    this.correctCharsTotal = 0;
    this.generateWords();
  }

  // ─── Live stats ───────────────────────────────────────────────────────────

  updateLiveStats(): void {
    let correct = 0;
    let total = 0;

    for (let i = 0; i < this.wordIndex; i++) {
      const w = this.words[i];
      total += w.typed.length + 1;
      for (let j = 0; j < w.typed.length; j++) {
        if (w.typed[j] === w.target[j]) correct++;
      }
      if (w.typed === w.target) correct++;
    }

    const elapsed = (this.selectedTime - this.timeLeft) / 60 || 0.001;
    this.liveWpm = elapsed > 0 ? Math.round((correct / 5) / elapsed) : 0;
    this.liveAccuracy = total > 0 ? Math.round((correct / total) * 100) + '%' : '—';
    this.progress = Math.min(100, (this.wordIndex / 80) * 100);
    this.correctCharsTotal = correct;
  }

  // ─── Template helpers ─────────────────────────────────────────────────────

  /**
   * Returns CSS class(es) for each character cell.
   * Handles: correct, wrong, extra (beyond word length), pending.
   */
  getCharClass(wordIdx: number, charIdx: number): string {
    const w = this.words[wordIdx];
    const typed = w.typed[charIdx];

    if (typed === undefined) return 'char pending';
    if (charIdx >= w.target.length) return 'char extra';
    return typed === w.target[charIdx] ? 'char correct' : 'char wrong';
  }

  /**
   * Returns an array of character objects for a given word,
   * including any extra characters typed beyond the word length.
   */
  getChars(wordIdx: number): { display: string; class: string; isCursor: boolean }[] {
    const w = this.words[wordIdx];
    const len = Math.max(w.target.length, w.typed.length);
    const result = [];

    for (let i = 0; i < len; i++) {
      const typedChar = w.typed[i];
      const targetChar = w.target[i] ?? '';
      let cls = 'char ';
      let display = targetChar;

      if (typedChar === undefined) {
        cls += 'pending';
      } else if (i >= w.target.length) {
        cls += 'extra';
        display = typedChar;
      } else {
        cls += typedChar === targetChar ? 'correct' : 'wrong';
      }

      const isCursor = wordIdx === this.wordIndex && i === w.typed.length;
      result.push({ display, class: cls, isCursor });
    }

    // Trailing cursor when caret is past all chars
    if (wordIdx === this.wordIndex && w.typed.length >= len) {
      result.push({ display: '', class: 'char', isCursor: true });
    }

    return result;
  }

  isWordWrong(wordIdx: number): boolean {
    const w = this.words[wordIdx];
    return wordIdx < this.wordIndex && w.typed !== w.target;
  }

  /** Chart bar heights (px) normalised to max WPM in history. */
  get chartBars(): number[] {
    const max = Math.max(...this.wpmHistory, 1);
    return this.wpmHistory.map(v => Math.max(2, Math.round((v / max) * 60)));
  }

  // ─── Private helpers ──────────────────────────────────────────────────────

  private clearTimer(): void {
    if (this.timerInterval !== null) {
      clearInterval(this.timerInterval);
      this.timerInterval = null;
    }
  }

  private scrollToCurrentWord(): void {
    const el = document.getElementById(`word-${this.wordIndex}`);
    const box = document.getElementById('words-box');
    if (!el || !box) return;
    const offset = el.getBoundingClientRect().top - box.getBoundingClientRect().top;
    if (offset > 88) box.scrollTop += offset - 44;
  }
}