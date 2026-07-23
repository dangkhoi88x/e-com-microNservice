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
