export function downloadBase64File(base64Data: string, fileName: string, contentType: string): void {
  const link = document.createElement('a');
  link.download = fileName;
  link.href = `data:${contentType};base64,${base64Data}`;
  link.click();
  link.remove();
}
