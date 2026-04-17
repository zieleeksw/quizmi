import { Component, ElementRef, HostListener, Input, ViewChild, forwardRef, inject } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR } from '@angular/forms';

import { sanitizeRichTextHtml } from '../../rich-text/rich-text.utils';

type FormattingState = {
  bold: boolean;
  italic: boolean;
  underline: boolean;
  colorActive: boolean;
};

@Component({
  selector: 'app-rich-text-editor',
  standalone: true,
  templateUrl: './rich-text-editor.component.html',
  styleUrl: './rich-text-editor.component.scss',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => RichTextEditorComponent),
      multi: true
    }
  ]
})
export class RichTextEditorComponent implements ControlValueAccessor {
  private readonly hostRef = inject<ElementRef<HTMLElement>>(ElementRef);

  @Input() placeholder = '';
  @Input() minHeight = '10rem';
  @Input() ariaLabel = 'Rich text editor';
  @Input() toolbarLabel = '';

  @ViewChild('editor', { static: true }) private editorRef?: ElementRef<HTMLDivElement>;

  protected readonly presetColors = ['#2d2d2d', '#d96b43', '#226c63', '#3454d1', '#8c3bd9', '#d6456a', '#d98f2b', '#ffffff'];
  protected disabled = false;
  protected selectedColor = '#2d2d2d';
  protected boldActive = false;
  protected italicActive = false;
  protected underlineActive = false;
  protected colorActive = false;
  protected isColorPaletteOpen = false;

  private value = '';
  private savedSelection: Range | null = null;
  private pendingCollapsedState: FormattingState | null = null;
  private toolbarInteractionInProgress = false;
  private onChange: (value: string) => void = () => undefined;
  private onTouched: () => void = () => undefined;

  writeValue(value: string | null): void {
    this.value = sanitizeRichTextHtml(value);
    this.renderValue();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
  }

  protected handleInput(): void {
    const sanitized = sanitizeRichTextHtml(this.editorElement().innerHTML);

    if (sanitized !== this.editorElement().innerHTML) {
      this.editorElement().innerHTML = sanitized;
      this.placeCaretAtEnd();
    }

    this.savedSelection = this.captureCurrentRange();
    this.pendingCollapsedState = null;
    this.commitValue(sanitized, false);
    this.updateToolbarState();
  }

  protected handleBlur(): void {
    this.pendingCollapsedState = null;
    this.commitValue(sanitizeRichTextHtml(this.editorElement().innerHTML), true);
    this.updateToolbarState();
    this.onTouched();
  }

  protected handleSelectionChange(): void {
    const selection = window.getSelection();

    if (!selection?.rangeCount || !this.selectionBelongsToEditor(selection)) {
      if (this.toolbarInteractionInProgress) {
        return;
      }

      this.savedSelection = null;
      this.pendingCollapsedState = null;
      this.updateToolbarState(false);
      return;
    }

    const nextRange = selection.getRangeAt(0).cloneRange();

    if (this.savedSelection && !this.areRangesEqual(this.savedSelection, nextRange)) {
      this.pendingCollapsedState = null;
    }

    this.savedSelection = nextRange;
    this.updateToolbarState();
  }

  @HostListener('document:selectionchange')
  protected handleDocumentSelectionChange(): void {
    this.handleSelectionChange();
  }

  protected handleKeydown(event: KeyboardEvent): void {
    if (event.key === ' ' && !event.ctrlKey && !event.altKey && !event.metaKey) {
      event.preventDefault();
      this.insertTextAtSelection('\u00A0');
      return;
    }

    if (event.key !== 'Enter' || event.shiftKey) {
      return;
    }

    event.preventDefault();
    this.applyCommand('insertLineBreak', undefined, false);
  }

  protected applyInlineCommand(command: 'bold' | 'italic' | 'underline'): void {
    this.isColorPaletteOpen = false;
    const currentState = this.currentToolbarState();
    const range = this.currentEditorRange();

    if (!range) {
      this.finishToolbarInteraction();
      return;
    }

    if (range.collapsed && currentState[command]) {
      this.clearCollapsedInlineFormat(command, range);
      this.finishToolbarInteraction();
      return;
    }

    this.applyCommand(command, undefined, false);

    if (range.collapsed) {
      this.pendingCollapsedState = {
        ...currentState,
        [command]: !currentState[command]
      };
      this.savedSelection = this.captureCurrentRange();
      this.updateToolbarState();
      this.finishToolbarInteraction();
      return;
    }

    this.pendingCollapsedState = null;
    this.finishToolbarInteraction();
  }

  protected handleColorButtonClick(): void {
    const currentState = this.currentToolbarState();
    const range = this.currentEditorRange();

    if (!range) {
      this.finishToolbarInteraction();
      return;
    }

    if (!currentState.colorActive) {
      this.isColorPaletteOpen = false;
      this.applyColor(this.selectedColor, currentState, range);
      this.finishToolbarInteraction();
      return;
    }

    if (!this.isColorPaletteOpen) {
      this.isColorPaletteOpen = true;
      this.finishToolbarInteraction();
      return;
    }

    this.isColorPaletteOpen = false;
    if (currentState.colorActive) {
      this.clearSelectedColor(range);
      this.finishToolbarInteraction();
      return;
    }
    this.finishToolbarInteraction();
  }

  protected selectPresetColor(color: string, event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    this.selectedColor = color;
    this.isColorPaletteOpen = false;
    const currentState = this.currentToolbarState();
    const range = this.currentEditorRange();

    if (range) {
      this.applyColor(color, currentState, range);
    }

    this.finishToolbarInteraction();
  }

  @HostListener('document:mousedown', ['$event'])
  protected handleDocumentMouseDown(event: MouseEvent): void {
    if (!this.isColorPaletteOpen) {
      return;
    }

    const target = event.target;

    if (target instanceof Node && this.hostRef.nativeElement.contains(target)) {
      return;
    }

    this.isColorPaletteOpen = false;
  }

  protected preserveSelection(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.toolbarInteractionInProgress = true;
    this.restoreSelection();
    this.editorElement().focus();
  }

  protected handlePaste(event: ClipboardEvent): void {
    event.preventDefault();

    const text = event.clipboardData?.getData('text/plain') ?? '';

    if (!text) {
      return;
    }

    this.restoreSelection();
    document.execCommand('insertHTML', false, escapeHtml(text).replace(/\r?\n/g, '<br>'));
    this.savedSelection = this.captureCurrentRange();
    this.handleInput();
  }

  private applyCommand(command: string, value?: string, styleWithCss = false): void {
    this.restoreSelection();
    this.editorElement().focus();
    document.execCommand('styleWithCSS', false, styleWithCss ? 'true' : 'false');
    document.execCommand(command, false, value);
    this.savedSelection = this.captureCurrentRange();
    this.handleInput();
    this.updateToolbarState();
  }

  private applyColor(color: string, currentState: FormattingState, range: Range): void {
    this.restoreSelection();
    this.editorElement().focus();
    document.execCommand('styleWithCSS', false, 'true');
    document.execCommand('foreColor', false, color);
    this.savedSelection = this.captureCurrentRange();
    this.handleInput();

    if (range.collapsed) {
      this.pendingCollapsedState = {
        ...currentState,
        colorActive: true
      };
      this.savedSelection = this.captureCurrentRange();
    } else {
      this.pendingCollapsedState = null;
    }

    this.updateToolbarState();
  }

  private insertTextAtSelection(text: string): void {
    const range = this.currentEditorRange();

    if (!range) {
      return;
    }

    this.restoreSelection();
    this.editorElement().focus();

    const activeRange = this.currentEditorRange();

    if (!activeRange) {
      return;
    }

    activeRange.deleteContents();

    const textNode = document.createTextNode(text);
    activeRange.insertNode(textNode);

    const nextRange = document.createRange();
    nextRange.setStartAfter(textNode);
    nextRange.collapse(true);

    const selection = window.getSelection();

    if (selection) {
      selection.removeAllRanges();
      selection.addRange(nextRange);
    }

    this.savedSelection = nextRange.cloneRange();
    this.handleInput();
    this.updateToolbarState();
  }

  private restoreSelection(): void {
    const selection = window.getSelection();

    if (!selection || !this.savedSelection) {
      return;
    }

    selection.removeAllRanges();
    selection.addRange(this.savedSelection);
  }

  private selectionBelongsToEditor(selection: Selection): boolean {
    const anchorNode = selection.anchorNode;
    return !!anchorNode && this.editorElement().contains(anchorNode);
  }

  private renderValue(): void {
    if (!this.editorRef) {
      return;
    }

    this.editorElement().innerHTML = this.value;
    this.updateToolbarState();
  }

  private commitValue(nextValue: string, shouldRender: boolean): void {
    const sanitized = sanitizeRichTextHtml(nextValue);

    this.value = sanitized;

    if (shouldRender && this.editorElement().innerHTML !== sanitized) {
      this.editorElement().innerHTML = sanitized;
    }

    this.onChange(sanitized);
  }

  private placeCaretAtEnd(): void {
    const selection = window.getSelection();

    if (!selection) {
      return;
    }

    const range = document.createRange();
    range.selectNodeContents(this.editorElement());
    range.collapse(false);
    selection.removeAllRanges();
    selection.addRange(range);
    this.savedSelection = range.cloneRange();
  }

  private editorElement(): HTMLDivElement {
    if (!this.editorRef) {
      throw new Error('Rich text editor is not available.');
    }

    return this.editorRef.nativeElement;
  }

  private updateToolbarState(shouldReadEditorState = true): void {
    if (this.disabled || !shouldReadEditorState) {
      this.applyToolbarState(this.createEmptyFormattingState());
      return;
    }

    const range = this.currentEditorRange();

    if (!range) {
      this.applyToolbarState(this.createEmptyFormattingState());
      return;
    }

    const domState = this.readFormattingState(range);
    const nextState = range.collapsed && this.pendingCollapsedState
      ? { ...domState, ...this.pendingCollapsedState }
      : domState;

    this.applyToolbarState(nextState);
  }

  private clearSelectedColor(range: Range): void {
    this.restoreSelection();
    this.editorElement().focus();

    if (range.collapsed) {
      this.pendingCollapsedState = {
        ...this.currentToolbarState(),
        colorActive: false
      };
      this.moveCaretOutsideColor(range);
      this.savedSelection = this.captureCurrentRange();
      this.updateToolbarState();
      return;
    }

    this.removeColorFromRange(range);
    this.pendingCollapsedState = null;
    this.savedSelection = this.captureCurrentRange();
    this.handleInput();
  }

  private currentEditorRange(): Range | null {
    const selection = window.getSelection();

    if (selection?.rangeCount && this.selectionBelongsToEditor(selection)) {
      return selection.getRangeAt(0);
    }

    if (this.savedSelection && this.editorElement().contains(this.savedSelection.startContainer)) {
      return this.savedSelection;
    }

    return null;
  }

  private readFormattingState(range: Range): FormattingState {
    const state = this.createEmptyFormattingState();
    let currentNode: Node | null = this.referenceNodeFromRange(range);

    while (currentNode && currentNode !== this.editorElement()) {
      if (currentNode.nodeType === Node.ELEMENT_NODE) {
        const element = currentNode as HTMLElement;
        const tagName = element.tagName.toLowerCase();
        const fontWeight = element.style.fontWeight.trim();
        const fontStyle = element.style.fontStyle.trim();
        const textDecoration = element.style.textDecoration.trim();

        if (tagName === 'strong' || tagName === 'b' || fontWeight === 'bold' || Number.parseInt(fontWeight, 10) >= 600) {
          state.bold = true;
        }

        if (tagName === 'em' || tagName === 'i' || fontStyle === 'italic') {
          state.italic = true;
        }

        if (tagName === 'u' || textDecoration.includes('underline')) {
          state.underline = true;
        }

        if (this.readColorValue(element)) {
          state.colorActive = true;
        }
      }

      currentNode = currentNode.parentNode;
    }

    return state;
  }

  private currentToolbarState(): FormattingState {
    return {
      bold: this.boldActive,
      italic: this.italicActive,
      underline: this.underlineActive,
      colorActive: this.colorActive
    };
  }

  private applyToolbarState(state: FormattingState): void {
    this.boldActive = state.bold;
    this.italicActive = state.italic;
    this.underlineActive = state.underline;
    this.colorActive = state.colorActive;
  }

  private createEmptyFormattingState(): FormattingState {
    return {
      bold: false,
      italic: false,
      underline: false,
      colorActive: false
    };
  }

  private captureCurrentRange(): Range | null {
    const selection = window.getSelection();

    if (!selection?.rangeCount || !this.selectionBelongsToEditor(selection)) {
      return null;
    }

    return selection.getRangeAt(0).cloneRange();
  }

  private areRangesEqual(left: Range, right: Range): boolean {
    return left.startContainer === right.startContainer
      && left.startOffset === right.startOffset
      && left.endContainer === right.endContainer
      && left.endOffset === right.endOffset;
  }

  private referenceNodeFromRange(range: Range): Node {
    const { startContainer, startOffset } = range;

    if (startContainer.nodeType === Node.TEXT_NODE) {
      return startContainer;
    }

    const element = startContainer as Element;

    if (!element.childNodes.length) {
      return element;
    }

    if (startOffset > 0) {
      return element.childNodes[Math.min(startOffset - 1, element.childNodes.length - 1)];
    }

    return element.childNodes[0] ?? element;
  }

  private readColorValue(element: HTMLElement): string | null {
    const color = element.style.color.trim();
    return color ? color : null;
  }

  private clearCollapsedInlineFormat(command: 'bold' | 'italic' | 'underline', range: Range): void {
    this.restoreSelection();
    this.editorElement().focus();

    this.pendingCollapsedState = {
      ...this.currentToolbarState(),
      [command]: false
    };

    const formatElement = this.closestInlineFormatElement(this.referenceNodeFromRange(range), command);

    if (!formatElement) {
      this.updateToolbarState();
      return;
    }

    this.moveCaretOutsideElement(range, formatElement);
    this.savedSelection = this.captureCurrentRange();
    this.updateToolbarState();
  }

  private moveCaretOutsideColor(range: Range): void {
    const colorElement = this.closestColorElement(this.referenceNodeFromRange(range));

    if (!colorElement) {
      return;
    }

    this.moveCaretOutsideElement(range, colorElement);
  }

  private moveCaretOutsideElement(range: Range, targetElement: HTMLElement): void {
    if (!targetElement.parentNode) {
      return;
    }

    const marker = document.createTextNode('');
    const splitRange = range.cloneRange();
    splitRange.insertNode(marker);

    const parent = targetElement.parentNode;
    const trailingNodes: Node[] = [];
    let current = marker.nextSibling;

    while (current) {
      const next = current.nextSibling;
      trailingNodes.push(current);
      current = next;
    }

    let trailingColorElement: HTMLElement | null = null;

    if (trailingNodes.length) {
      trailingColorElement = targetElement.cloneNode(false) as HTMLElement;
      trailingNodes.forEach((node) => trailingColorElement!.appendChild(node));
      parent.insertBefore(trailingColorElement, targetElement.nextSibling);
    }

    parent.insertBefore(marker, trailingColorElement ?? targetElement.nextSibling);

    if (!targetElement.textContent?.length) {
      parent.removeChild(targetElement);
    }

    if (trailingColorElement && !trailingColorElement.textContent?.length) {
      parent.removeChild(trailingColorElement);
      trailingColorElement = null;
    }

    const selection = window.getSelection();

    if (!selection) {
      return;
    }

    const nextRange = document.createRange();
    nextRange.setStartAfter(marker);
    nextRange.collapse(true);
    selection.removeAllRanges();
    selection.addRange(nextRange);
    marker.remove();
  }

  private removeColorFromRange(range: Range): void {
    const extracted = range.extractContents();
    const cleaned = this.stripColorFromFragment(extracted);
    const nodes = Array.from(cleaned.childNodes);

    range.insertNode(cleaned);

    if (!nodes.length) {
      return;
    }

    const selection = window.getSelection();

    if (!selection) {
      return;
    }

    const nextRange = document.createRange();
    nextRange.setStartBefore(nodes[0]);
    nextRange.setEndAfter(nodes[nodes.length - 1]);
    selection.removeAllRanges();
    selection.addRange(nextRange);
  }

  private stripColorFromFragment(fragment: DocumentFragment): DocumentFragment {
    Array.from(fragment.childNodes).forEach((node) => this.stripColorFromNode(node));
    return fragment;
  }

  private stripColorFromNode(node: Node): void {
    if (node.nodeType !== Node.ELEMENT_NODE) {
      return;
    }

    const element = node as HTMLElement;
    Array.from(element.childNodes).forEach((child) => this.stripColorFromNode(child));

    if (element.style.color) {
      element.style.removeProperty('color');
    }

    if (element.getAttribute('color')) {
      element.removeAttribute('color');
    }

    if (element.tagName.toLowerCase() === 'span' && !element.getAttribute('style')) {
      element.replaceWith(...Array.from(element.childNodes));
    }
  }

  private closestColorElement(node: Node | null): HTMLElement | null {
    let current: Node | null = node;

    while (current && current !== this.editorElement()) {
      if (current.nodeType === Node.ELEMENT_NODE) {
        const element = current as HTMLElement;

        if (this.readColorValue(element)) {
          return element;
        }
      }

      current = current.parentNode;
    }

    return null;
  }

  private closestInlineFormatElement(
    node: Node | null,
    command: 'bold' | 'italic' | 'underline'
  ): HTMLElement | null {
    let current: Node | null = node;

    while (current && current !== this.editorElement()) {
      if (current.nodeType === Node.ELEMENT_NODE) {
        const element = current as HTMLElement;

        if (this.matchesInlineFormat(element, command)) {
          return element;
        }
      }

      current = current.parentNode;
    }

    return null;
  }

  private matchesInlineFormat(element: HTMLElement, command: 'bold' | 'italic' | 'underline'): boolean {
    const tagName = element.tagName.toLowerCase();

    switch (command) {
      case 'bold':
        return tagName === 'strong'
          || tagName === 'b'
          || element.style.fontWeight.trim() === 'bold'
          || Number.parseInt(element.style.fontWeight.trim(), 10) >= 600;
      case 'italic':
        return tagName === 'em'
          || tagName === 'i'
          || element.style.fontStyle.trim() === 'italic';
      case 'underline':
        return tagName === 'u'
          || element.style.textDecoration.trim().includes('underline');
    }
  }

  private finishToolbarInteraction(): void {
    queueMicrotask(() => {
      this.toolbarInteractionInProgress = false;
    });
  }
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}
