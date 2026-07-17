import { Box, Button, FormControl, IconButton, InputLabel, MenuItem, Select, Stack, Switch, TextField, Typography } from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";

export const emptyOption = () => ({ name: "", displayName: "", displayType: "BUTTON", displayOrder: 0, required: true, values: [] });
export const emptyOptionValue = () => ({ value: "", displayValue: "", colorHex: "", imageUrl: "", displayOrder: 0, active: true });

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

export function ProductOptionsEditor({ options, onChange }) {
  const updateOption = (index, field, value) => onChange(options.map((option, i) => i === index ? { ...option, [field]: value } : option));
  const addValue = (index) => updateOption(index, "values", [...options[index].values, emptyOptionValue()]);
  const updateValue = (optionIndex, valueIndex, field, value) => updateOption(optionIndex, "values", options[optionIndex].values.map((item, i) => i === valueIndex ? { ...item, [field]: value } : item));

  return <Box>
    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={1.5}>
      <Box><Typography className="form-section-kicker">Product options</Typography><Typography variant="body2" color="text.secondary">Define color, size, storage, or any selectable option.</Typography></Box>
      <Button type="button" variant="outlined" size="small" startIcon={<AddOutlinedIcon />} onClick={() => onChange([...options, emptyOption()])}>Add option</Button>
    </Stack>
    <Stack spacing={2}>
      {options.length === 0 && <Box className="variant-empty-state">No options. Add options before creating variants.</Box>}
      {options.map((option, optionIndex) => <Box key={`option-${optionIndex}`} sx={{ p: 2, border: "1px solid", borderColor: "divider", borderRadius: 2 }}>
        <Stack direction={{ xs: "column", md: "row" }} spacing={1.5} alignItems={{ md: "center" }}>
          <TextField label="Name" helperText="Technical key: color, size..." value={option.name} onChange={(e) => updateOption(optionIndex, "name", e.target.value)} required />
          <TextField label="Display name" value={option.displayName} onChange={(e) => updateOption(optionIndex, "displayName", e.target.value)} required />
          <FormControl sx={{ minWidth: 170 }}><InputLabel>Display type</InputLabel><Select label="Display type" value={option.displayType} onChange={(e) => updateOption(optionIndex, "displayType", e.target.value)}><MenuItem value="BUTTON">Button</MenuItem><MenuItem value="COLOR_SWATCH">Color swatch</MenuItem><MenuItem value="DROPDOWN">Dropdown</MenuItem></Select></FormControl>
          <Stack direction="row" alignItems="center"><Switch checked={option.required} onChange={(e) => updateOption(optionIndex, "required", e.target.checked)} /><Typography>Required</Typography></Stack>
          <IconButton color="error" onClick={() => onChange(options.filter((_, i) => i !== optionIndex))}><DeleteOutlineOutlinedIcon /></IconButton>
        </Stack>
        <Stack spacing={1.25} mt={2}>
          {option.values.map((value, valueIndex) => <Stack key={`value-${valueIndex}`} direction={{ xs: "column", md: "row" }} spacing={1}>
            <TextField label="Value" value={value.value} onChange={(e) => updateValue(optionIndex, valueIndex, "value", e.target.value)} required />
            <TextField label="Display value" value={value.displayValue} onChange={(e) => updateValue(optionIndex, valueIndex, "displayValue", e.target.value)} required />
            {option.displayType === "COLOR_SWATCH" && <TextField label="Color hex" value={value.colorHex} onChange={(e) => updateValue(optionIndex, valueIndex, "colorHex", e.target.value)} />}
            <TextField label="Image URL" value={value.imageUrl} onChange={(e) => updateValue(optionIndex, valueIndex, "imageUrl", e.target.value)} />
            <IconButton color="error" onClick={() => updateOption(optionIndex, "values", option.values.filter((_, i) => i !== valueIndex))}><DeleteOutlineOutlinedIcon /></IconButton>
          </Stack>)}
          <Button type="button" size="small" startIcon={<AddOutlinedIcon />} onClick={() => addValue(optionIndex)} sx={{ alignSelf: "flex-start" }}>Add value</Button>
        </Stack>
      </Box>)}
    </Stack>
  </Box>;
}

export function VariantAttributeFields({ options, attributes, onChange }) {
  return options.map((option) => <FormControl key={option.name} sx={{ minWidth: 150 }} required={option.required}>
    <InputLabel>{option.displayName || option.name}</InputLabel>
    <Select label={option.displayName || option.name} value={attributes?.[option.name] || ""} onChange={(e) => onChange(option.name, e.target.value)}>
      {option.values.filter((value) => value.active !== false).map((value) => <MenuItem key={value.value} value={value.value}>{value.displayValue || value.value}</MenuItem>)}
    </Select>
  </FormControl>);
}
