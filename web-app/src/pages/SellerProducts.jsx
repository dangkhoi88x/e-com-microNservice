import { useCallback, useEffect, useState } from "react";
import { Alert, Box, Button, Chip, CircularProgress, Paper, Stack, Table, TableBody, TableCell, TableHead, TableRow, Typography } from "@mui/material";
import AddOutlinedIcon from "@mui/icons-material/AddOutlined";
import DeleteOutlineOutlinedIcon from "@mui/icons-material/DeleteOutlineOutlined";
import SendOutlinedIcon from "@mui/icons-material/SendOutlined";
import { useNavigate } from "react-router-dom";
import { PageHeader } from "../components/admin";
import MainLayout from "../layouts/MainLayout";
import { deleteSellerProduct, getMySellerProducts, submitSellerProduct } from "../services/productService";

const tone = (status) => ({
  DRAFT: "default", PENDING_APPROVAL: "warning", ACTIVE: "success", REJECTED: "error", INACTIVE: "default",
}[status] || "default");

export default function SellerProducts() {
  const navigate = useNavigate();
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const data = await getMySellerProducts({ page: 1, size: 50 });
      setProducts(data.content || []);
      setError("");
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể tải sản phẩm của shop.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const submit = async (product) => {
    try {
      await submitSellerProduct(product.id);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Không thể gửi sản phẩm để duyệt.");
    }
  };

  const remove = async (product) => {
    if (!window.confirm(`Xóa bản nháp “${product.name}”?`)) return;
    try {
      await deleteSellerProduct(product.id);
      await load();
    } catch (requestError) {
      setError(requestError.response?.data?.message || "Chỉ có thể xóa sản phẩm đang là bản nháp hoặc đã bị từ chối.");
    }
  };

  return <MainLayout>
    <PageHeader eyebrow="Seller center" title="Sản phẩm của shop" description="Tạo bản nháp, gửi duyệt và theo dõi kết quả kiểm duyệt." actions={<Button variant="contained" startIcon={<AddOutlinedIcon />} onClick={() => navigate("/seller/products/new")}>Tạo sản phẩm</Button>} />
    {error && <Alert severity="error" sx={{ mt: 3 }}>{error}</Alert>}
    <Paper sx={{ mt: 3, overflow: "hidden" }} elevation={0} className="admin-table-card">
      {loading ? <Box sx={{ display: "grid", placeItems: "center", minHeight: 200 }}><CircularProgress /></Box> : <Table>
        <TableHead><TableRow><TableCell>Sản phẩm</TableCell><TableCell>Giá</TableCell><TableCell>Kho</TableCell><TableCell>Trạng thái</TableCell><TableCell align="right">Thao tác</TableCell></TableRow></TableHead>
        <TableBody>{products.map((product) => <TableRow key={product.id}>
          <TableCell><Stack spacing={0.4}><Typography fontWeight={800}>{product.name}</Typography>{product.moderationNote && <Typography variant="caption" color="error">Lý do: {product.moderationNote}</Typography>}</Stack></TableCell>
          <TableCell>{Number(product.price || 0).toLocaleString("vi-VN")} đ</TableCell><TableCell>{product.quantity}</TableCell>
          <TableCell><Chip label={product.status} color={tone(product.status)} size="small" /></TableCell>
          <TableCell align="right">{["DRAFT", "REJECTED"].includes(product.status) && <><Button size="small" startIcon={<SendOutlinedIcon />} onClick={() => submit(product)}>Gửi duyệt</Button><Button size="small" color="error" startIcon={<DeleteOutlineOutlinedIcon />} onClick={() => remove(product)}>Xóa</Button></>}</TableCell>
        </TableRow>)}</TableBody>
      </Table>}
      {!loading && products.length === 0 && <Box sx={{ p: 5, textAlign: "center" }}><Typography fontWeight={700}>Shop chưa có sản phẩm</Typography><Typography variant="body2" color="text.secondary">Hãy tạo sản phẩm đầu tiên dưới dạng bản nháp.</Typography></Box>}
    </Paper>
  </MainLayout>;
}
