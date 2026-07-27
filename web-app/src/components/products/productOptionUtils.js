export const toOptionsPayload = (options) => options.map((option, optionIndex) => ({
  name: String(option.name || "").trim().toLowerCase(),
  displayName: String(option.displayName || "").trim(),
  displayType: option.displayType,
  displayOrder: Number(option.displayOrder) || optionIndex,
  required: option.required,
  values: option.values.map((value, valueIndex) => ({
    value: String(value.value || "").trim().toLowerCase(),
    displayValue: String(value.displayValue || "").trim(),
    colorHex: String(value.colorHex || "").trim() || null,
    imageUrl: String(value.imageUrl || "").trim() || null,
    displayOrder: Number(value.displayOrder) || valueIndex,
    active: value.active,
  })),
}));

export const resolveOptionImageUrls = async (options, uploadImage, onUploaded) => Promise.all(
  options.map(async (option) => ({
    ...option,
    values: await Promise.all(option.values.map(async (value) => {
      if (!value.imageFile) return value;
      const media = await uploadImage(value.imageFile);
      onUploaded(media);
      return { ...value, imageUrl: media.contentUrl };
    })),
  })),
);
