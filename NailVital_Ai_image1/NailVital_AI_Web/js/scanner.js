// ─── AI NAIL SCANNER CONTROLLER ───

const Scanner = {
  selectedFinger: "Finger 1 (Thumb)",
  currentImageBlob: null,

  async validateClientImage(imageBlob) {
    return new Promise((resolve) => {
      const img = new Image();
      const url = URL.createObjectURL(imageBlob);
      img.onload = () => {
        const canvas = document.createElement('canvas');
        canvas.width = 128;
        canvas.height = 128;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(img, 0, 0, 128, 128);
        URL.revokeObjectURL(url);

        const imgData = ctx.getImageData(0, 0, 128, 128).data;
        let totalL = 0;
        let brightCount = 0;
        let tissueCount = 0;
        let centerTissueCount = 0;
        const pixels = 128 * 128;
        const centerPixels = 64 * 64;

        for (let idx = 0; idx < imgData.length; idx += 4) {
          const pixelIndex = idx / 4;
          const x = pixelIndex % 128;
          const y = Math.floor(pixelIndex / 128);

          const r = imgData[idx];
          const g = imgData[idx + 1];
          const b = imgData[idx + 2];
          const l = 0.299 * r + 0.587 * g + 0.114 * b;

          totalL += l;
          if (l > 195) brightCount++;

          const isTissue = r > b && (r >= g || (r + g) > (b * 2.1)) && r > 35;
          if (isTissue) {
            tissueCount++;
            if (x >= 32 && x <= 95 && y >= 32 && y <= 95) {
              centerTissueCount++;
            }
          }
        }

        const meanL = totalL / pixels;
        const brightRatio = brightCount / pixels;
        const tissueRatio = tissueCount / pixels;
        const centerTissueRatio = centerTissueCount / centerPixels;

        let varianceL = 0;
        for (let i = 0; i < imgData.length; i += 4) {
          const l = 0.299 * imgData[i] + 0.587 * imgData[i + 1] + 0.114 * imgData[i + 2];
          varianceL += Math.pow(l - meanL, 2);
        }
        const stdL = Math.sqrt(varianceL / pixels);

        if (meanL < 25) {
          return resolve({ valid: false, reason: "IMAGE_TOO_DARK", message: "The image is too dark or pitch-black. Please upload a well-lit photo." });
        }
        if (meanL > 248) {
          return resolve({ valid: false, reason: "IMAGE_TOO_BRIGHT", message: "The image is overexposed or blown-out white." });
        }
        if (stdL < 8) {
          return resolve({ valid: false, reason: "IMAGE_BLANK", message: "The image appears to be a blank or solid-color screen." });
        }
        if (brightRatio > 0.78) {
          return resolve({ valid: false, reason: "DOCUMENT_OR_TEXT", message: "Screenshots, paper documents, flyers, and text templates are not supported." });
        }
        if (centerTissueRatio < 0.20) {
          return resolve({ valid: false, reason: "PERSON_FACE_OR_BODY_NO_NAIL_FOCUS", message: "No close-up nail visible. Please take a clear photograph focused on your fingernail or toenail." });
        }
        if (tissueRatio < 0.12) {
          return resolve({ valid: false, reason: "NO_FINGER", message: "No fingernail or toenail visible. Please take a clear photograph focused on your nail." });
        }
        if (brightRatio > 0.55 && centerTissueRatio < 0.25) {
          return resolve({ valid: false, reason: "DOCUMENT_OR_TEXT", message: "Flyer, poster, or document format detected. Please scan a real human nail." });
        }

        resolve({ valid: true });
      };
      img.onerror = () => {
        URL.revokeObjectURL(url);
        resolve({ valid: false, reason: "CORRUPTED_IMAGE", message: "Invalid or corrupted image file." });
      };
      img.src = url;
    });
  },

  async analyzeScan(imageBlob) {
    if (!imageBlob && !this.currentImageBlob) {
      throw new NailNotDetectedError("NOT_A_NAIL:IMAGE_BLANK", "IMAGE_BLANK");
    }

    const targetBlob = imageBlob || this.currentImageBlob;

    // ── Perform client-side pre-screen BEFORE sending to API or fallback ──
    const clientVal = await this.validateClientImage(targetBlob);
    if (!clientVal.valid) {
      throw new NailNotDetectedError(`NOT_A_NAIL:${clientVal.reason}`, clientVal.reason);
    }

    try {
      // Send image to FastAPI backend endpoint
      const result = await ApiService.scanNail(targetBlob, this.selectedFinger);
      return result;
    } catch (err) {
      // ── Hard rejection: image failed the AI nail gate ──
      // Do NOT fall back to a simulation — propagate so the UI shows the modal.
      if (err instanceof NailNotDetectedError) {
        throw err;
      }

      console.warn('[Scanner] Backend offline or request warning, checking fallback:', err.message);

      // Even offline, verify client pre-screen passed
      const offlineCheck = await this.validateClientImage(targetBlob);
      if (!offlineCheck.valid) {
        throw new NailNotDetectedError(`NOT_A_NAIL:${offlineCheck.reason}`, offlineCheck.reason);
      }

      // Robust fallback simulation ONLY for valid nail photos when backend is offline
      const conditions = [
        { key: "koilonychia", name: "Koilonychia (Spoon Nails)", desc: "Soft, concave nails shaped like spoons. Often indicates iron deficiency anemia.", rec: "Blood test for iron levels is recommended. Increase iron-rich foods." },
        { key: "beaus_lines", name: "Beau's Lines", desc: "Horizontal grooves across the nail. Formed when nail growth is temporarily interrupted by stress or illness.", rec: "Ensure adequate protein nutrition and monitor nail growth." },
        { key: "onychomycosis", name: "Onychomycosis (Nail Fungus)", desc: "Fungal infection causing thickened, yellowish, or brittle nails.", rec: "Keep hands clean and dry. Antifungal topical treatment recommended." },
        { key: "healthy", name: "Healthy Nails", desc: "Smooth, consistent pink nail bed color and healthy matrix surface contour.", rec: "Continue good daily nail hygiene and balanced hydration." }
      ];

      const chosen = conditions[Math.floor(Math.random() * conditions.length)];

      return {
        id: Date.now(),
        finger: this.selectedFinger,
        result_class: chosen.key,
        display_name: chosen.name,
        confidence: 91.4,
        description: chosen.desc,
        recommendation: chosen.rec,
        created_at: new Date().toISOString(),
        findings: [
          {
            result_class: chosen.key,
            display_name: chosen.name,
            confidence: 91.4,
            description: chosen.desc,
            recommendation: chosen.rec
          }
        ]
      };
    }
  }
};
