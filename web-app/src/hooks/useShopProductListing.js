import { useCallback, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router-dom";
import {
  getProductAggregations,
  searchProductSuggestions,
  searchProducts,
} from "../services/productService";

export default function useShopProductListing({ categoryId, size = 24 } = {}) {
  const [urlParams, setUrlParams] = useSearchParams();
  const [query, setQuery] = useState(urlParams.get("q") || "");
  const [products, setProducts] = useState([]);
  const [suggestions, setSuggestions] = useState([]);
  const [aggregation, setAggregation] = useState({ categories: [], priceStats: {} });
  const [pageInfo, setPageInfo] = useState({ currentPage: 1, totalPages: 0, totalElements: 0 });
  const [loading, setLoading] = useState(false);

  const filters = useMemo(() => ({
    q: urlParams.get("q") || undefined,
    categoryId: categoryId || urlParams.get("categoryId") || undefined,
    minPrice: urlParams.get("minPrice") || undefined,
    maxPrice: urlParams.get("maxPrice") || undefined,
    minRating: urlParams.get("minRating") || undefined,
    inStock: urlParams.get("inStock") === "true" ? true : undefined,
    sort: urlParams.get("sort") || "createdAt,desc",
    page: Math.max(1, Number(urlParams.get("page") || 1)),
    size,
  }), [categoryId, size, urlParams]);

  const setFilter = useCallback((key, value) => {
    setUrlParams((current) => {
      const next = new URLSearchParams(current);
      if (value === undefined || value === "" || value === false) next.delete(key);
      else next.set(key, String(value));
      if (key !== "page") next.delete("page");
      return next;
    });
  }, [setUrlParams]);

  useEffect(() => {
    setQuery(urlParams.get("q") || "");
  }, [urlParams]);

  useEffect(() => {
    setLoading(true);
    Promise.all([searchProducts(filters), getProductAggregations(filters)])
      .then(([result, facets]) => {
        setProducts(result.content || []);
        setPageInfo({
          currentPage: result.currentPage || filters.page,
          totalPages: result.totalPages || 0,
          totalElements: result.totalElements || 0,
        });
        setAggregation(facets || { categories: [], priceStats: {} });
      })
      .catch(() => {
        setProducts([]);
        setPageInfo({ currentPage: 1, totalPages: 0, totalElements: 0 });
      })
      .finally(() => setLoading(false));
  }, [filters]);

  useEffect(() => {
    const timer = window.setTimeout(() => {
      searchProductSuggestions(query)
        .then(setSuggestions)
        .catch(() => setSuggestions([]));
    }, 300);
    return () => window.clearTimeout(timer);
  }, [query]);

  const submitQuery = useCallback(() => {
    setFilter("q", query.trim());
    setSuggestions([]);
  }, [query, setFilter]);

  const chooseSuggestion = useCallback((item) => {
    setQuery(item.name);
    setFilter("q", item.name);
    setSuggestions([]);
  }, [setFilter]);

  const resetFilters = useCallback(() => {
    setQuery("");
    setSuggestions([]);
    setUrlParams({});
  }, [setUrlParams]);

  return {
    aggregation,
    chooseSuggestion,
    filters,
    loading,
    pageInfo,
    products,
    query,
    resetFilters,
    setFilter,
    setQuery,
    setSuggestions,
    submitQuery,
    suggestions,
    urlParams,
  };
}
