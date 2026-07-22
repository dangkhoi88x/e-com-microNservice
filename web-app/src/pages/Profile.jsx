import {
  Alert,
  Avatar,
  Box,
  Button,
  CircularProgress,
  Divider,
  Paper,
  Stack,
  TextField,
  Typography,
} from "@mui/material";
import AccountCircleOutlinedIcon from "@mui/icons-material/AccountCircleOutlined";
import CalendarMonthOutlinedIcon from "@mui/icons-material/CalendarMonthOutlined";
import FingerprintOutlinedIcon from "@mui/icons-material/FingerprintOutlined";
import ImageOutlinedIcon from "@mui/icons-material/ImageOutlined";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import RefreshOutlinedIcon from "@mui/icons-material/RefreshOutlined";
import SaveOutlinedIcon from "@mui/icons-material/SaveOutlined";
import CloudUploadOutlinedIcon from "@mui/icons-material/CloudUploadOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import { useEffect, useState } from "react";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { getMyProfile, updateMyProfile } from "../services/profileService";

const emptyProfile = {
  userId: "",
  firstName: "",
  lastName: "",
  avatarUrl: "",
  bio: "",
  birthDate: "",
};

export default function Profile() {
  const [profile, setProfile] = useState(emptyProfile);
  const [savedProfile, setSavedProfile] = useState(emptyProfile);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [successMessage, setSuccessMessage] = useState("");

  const fullName = [profile.firstName, profile.lastName]
    .filter(Boolean)
    .join(" ");

  const loadProfile = async () => {
    setLoading(true);
    setErrorMessage("");
    setSuccessMessage("");

    try {
      const data = await getMyProfile();
      const nextProfile = {
        userId: data?.userId || "",
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        avatarUrl: data?.avatarUrl || "",
        bio: data?.bio || "",
        birthDate: data?.birthDate || "",
      };
      setProfile(nextProfile);
      setSavedProfile(nextProfile);
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not load profile.",
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadProfile();
  }, []);

  const setField = (field) => (event) => {
    setProfile((current) => ({
      ...current,
      [field]: event.target.value,
    }));
  };

  const handleAvatarUpload = (event) => {
    const file = event.target.files?.[0];
    event.target.value = "";

    if (!file) return;
    if (!file.type.startsWith("image/")) {
      setErrorMessage("Vui lòng chọn tệp ảnh hợp lệ.");
      return;
    }
    if (file.size > 1024 * 1024) {
      setErrorMessage("Ảnh đại diện phải nhỏ hơn hoặc bằng 1 MB.");
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setProfile((current) => ({ ...current, avatarUrl: String(reader.result || "") }));
      setErrorMessage("");
    };
    reader.readAsDataURL(file);
  };

  const clearAvatar = () => setProfile((current) => ({ ...current, avatarUrl: "" }));

  const handleSubmit = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    setErrorMessage("");
    setSuccessMessage("");

    const payload = {
      firstName: profile.firstName.trim(),
      lastName: profile.lastName.trim(),
      avatarUrl: profile.avatarUrl.trim(),
      bio: profile.bio.trim(),
      birthDate: profile.birthDate || null,
    };

    try {
      const data = await updateMyProfile(payload);
      const nextProfile = {
        userId: data?.userId || profile.userId,
        firstName: data?.firstName || "",
        lastName: data?.lastName || "",
        avatarUrl: data?.avatarUrl || "",
        bio: data?.bio || "",
        birthDate: data?.birthDate || "",
      };
      setProfile(nextProfile);
      setSavedProfile(nextProfile);
      setSuccessMessage("Profile updated successfully.");
    } catch (error) {
      setErrorMessage(
        error.response?.data?.message || "Could not update profile.",
      );
    } finally {
      setSubmitting(false);
    }
  };

  const hasUnsavedChanges = ["firstName", "lastName", "avatarUrl", "bio", "birthDate"]
    .some((field) => (profile[field] || "") !== (savedProfile[field] || ""));

  return (
    <MainLayout>
      <PageHeader
        eyebrow="Account"
        title="Hồ sơ cá nhân"
        description="Quản lý thông tin hiển thị của tài khoản quản trị."
        actions={
        <Button
          variant="outlined"
          startIcon={<RefreshOutlinedIcon />}
          onClick={loadProfile}
        >
          Tải lại
        </Button>
        }
      />

      {loading ? (
        <Paper
          className="admin-data-panel profile-loading-card"
          elevation={0}
          sx={{
            mt: 3,
            p: 6,
            border: "1px solid",
            borderColor: "divider",
          }}
        >
          <Stack alignItems="center" spacing={2}>
            <CircularProgress />
            <Typography color="text.secondary">Đang tải hồ sơ...</Typography>
          </Stack>
        </Paper>
      ) : (
        <Box className="profile-layout">
          <Box className="profile-summary-column">
            <Paper
              className="profile-summary-card"
              elevation={0}
              sx={{ p: 3, border: "1px solid", borderColor: "divider" }}
            >
              <Stack alignItems="center" spacing={1.5} className="profile-identity">
                <Avatar
                  className="profile-avatar"
                  src={profile.avatarUrl || undefined}
                  sx={{ width: 96, height: 96, bgcolor: "primary.main" }}
                >
                  <AccountCircleOutlinedIcon sx={{ fontSize: 56 }} />
                </Avatar>
                <Box sx={{ textAlign: "center" }}>
                  <Typography className="profile-summary-kicker">TÀI KHOẢN HIỆN TẠI</Typography>
                  <Typography variant="h6" className="profile-summary-name">
                    {fullName || "Chưa cập nhật tên"}
                  </Typography>
                  <Stack direction="row" spacing={0.75} justifyContent="center" alignItems="center" className="profile-user-id">
                    <FingerprintOutlinedIcon />
                    <Typography>{profile.userId || "Chưa có mã tài khoản"}</Typography>
                  </Stack>
                </Box>
              </Stack>

              <Divider sx={{ my: 2 }} />

              <Stack spacing={1.25} className="profile-summary-details">
                <Box className="profile-detail-item">
                  <Box className="profile-detail-icon"><CalendarMonthOutlinedIcon /></Box>
                  <Box>
                    <Typography className="profile-detail-label">Ngày sinh</Typography>
                    <Typography className="profile-detail-value">{profile.birthDate || "Chưa cập nhật"}</Typography>
                  </Box>
                </Box>
                <Box className="profile-detail-item profile-bio-item">
                  <Box className="profile-detail-icon"><InfoOutlinedIcon /></Box>
                  <Box>
                    <Typography className="profile-detail-label">Giới thiệu</Typography>
                    <Typography className="profile-detail-value">{profile.bio || "Thêm vài dòng để cá nhân hóa hồ sơ của bạn."}</Typography>
                  </Box>
                </Box>
              </Stack>
            </Paper>
          </Box>

          <Box className="profile-form-column">
            <Paper
              className="profile-form-card"
              elevation={0}
              sx={{ p: 3, border: "1px solid", borderColor: "divider" }}
            >
              <Box className="profile-form-intro">
                <Typography className="form-section-kicker">Thông tin hiển thị</Typography>
                <Typography className="profile-form-title">Cập nhật hồ sơ</Typography>
                <Typography className="profile-form-description">
                  Các thông tin này sẽ được dùng để nhận diện tài khoản của bạn trong hệ thống.
                </Typography>
              </Box>
              {errorMessage && (
                <Alert severity="error" sx={{ mb: 2 }}>
                  {errorMessage}
                </Alert>
              )}
              {successMessage && (
                <Alert severity="success" sx={{ mb: 2 }}>
                  {successMessage}
                </Alert>
              )}

              <Box component="form" onSubmit={handleSubmit}>
                <Box className="profile-field-group">
                  <Box className="profile-group-heading">
                    <Typography className="profile-group-title">Danh tính</Typography>
                    <Typography className="profile-group-description">Tên và ảnh dùng để hiển thị tài khoản của bạn.</Typography>
                  </Box>
                  <Box className="profile-identity-fields">
                    <TextField
                      label="Họ"
                      value={profile.firstName}
                      onChange={setField("firstName")}
                      fullWidth
                      required
                    />
                    <TextField
                      label="Tên"
                      value={profile.lastName}
                      onChange={setField("lastName")}
                      fullWidth
                      required
                    />
                    <Box className="profile-upload-field">
                      <Box className="profile-upload-preview">
                        {profile.avatarUrl ? <img src={profile.avatarUrl} alt="Xem trước ảnh đại diện" /> : <ImageOutlinedIcon />}
                      </Box>
                      <Box className="profile-upload-copy">
                        <Typography className="profile-upload-title">Ảnh đại diện</Typography>
                        <Typography className="profile-upload-hint">PNG, JPG hoặc WebP · tối đa 1 MB</Typography>
                        <Stack direction="row" spacing={1} sx={{ mt: 1 }}>
                          <Button component="label" size="small" variant="outlined" startIcon={<CloudUploadOutlinedIcon />}>
                            Tải ảnh lên
                            <input hidden type="file" accept="image/png,image/jpeg,image/webp" onChange={handleAvatarUpload} />
                          </Button>
                          {profile.avatarUrl && <Button size="small" color="inherit" onClick={clearAvatar} startIcon={<DeleteOutlineOutlinedIcon />}>Xóa</Button>}
                        </Stack>
                      </Box>
                    </Box>
                  </Box>
                </Box>

                <Box className="profile-field-group">
                  <Box className="profile-group-heading">
                    <Typography className="profile-group-title">Thông tin cá nhân</Typography>
                    <Typography className="profile-group-description">Bổ sung ngày sinh và phần giới thiệu ngắn.</Typography>
                  </Box>
                  <Box className="profile-personal-fields">
                    <Box className="profile-date-field">
                      <Typography component="label" htmlFor="profile-birth-date">Ngày sinh</Typography>
                      <TextField
                      id="profile-birth-date"
                      type="date"
                      value={profile.birthDate}
                      onChange={setField("birthDate")}
                      fullWidth
                      />
                    </Box>
                    <TextField
                      label="Giới thiệu"
                      value={profile.bio}
                      onChange={setField("bio")}
                      fullWidth
                      multiline
                      minRows={5}
                      placeholder="Viết vài dòng ngắn về bạn..."
                    />
                  </Box>
                </Box>

                {hasUnsavedChanges && <Stack direction={{ xs: "column", sm: "row" }} justifyContent="space-between" alignItems={{ xs: "stretch", sm: "center" }} spacing={1.5} className="form-action-bar profile-sticky-save-bar">
                      <Typography className="profile-save-note">Bạn có thay đổi chưa được lưu.</Typography>
                      <Button
                        type="submit"
                        variant="contained"
                        startIcon={<SaveOutlinedIcon />}
                        disabled={submitting}
                      >
                        {submitting ? "Đang lưu..." : "Lưu thay đổi"}
                      </Button>
                </Stack>}
              </Box>
            </Paper>
          </Box>
        </Box>
      )}
    </MainLayout>
  );
}
