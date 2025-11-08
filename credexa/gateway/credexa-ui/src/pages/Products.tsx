import { useEffect, useState } from 'react';
import { productApi } from '@/services/api';
import type { Product } from '@/types';
import { isManagerOrAdmin } from '@/lib/utils';
import { Loader2 } from 'lucide-react';
import CustomerProductsView from '@/components/products/CustomerProductsView';
import AdminProductsView from '@/components/products/AdminProductsView';

export default function Products() {
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdmin, setIsAdmin] = useState(false);

  useEffect(() => {
    checkUserRole();
    fetchProducts();
  }, []);

  const checkUserRole = () => {
    setIsAdmin(isManagerOrAdmin());
  };

  const fetchProducts = async () => {
    try {
      setLoading(true);
      // Get active products for customers, all products for admin
      const response = isManagerOrAdmin() 
        ? await productApi.getAllProducts()
        : await productApi.getActiveProducts();
      // Backend returns ApiResponse wrapper; data may be:
      // - an array (for endpoints returning List<...>) OR
      // - a paginated object { products: [...] }
      const payload = response?.data?.data;
      let items: any[] = [];
      if (!payload) {
        items = [];
      } else if (Array.isArray(payload)) {
        items = payload;
      } else if (payload.products && Array.isArray(payload.products)) {
        items = payload.products;
      } else if (payload.items && Array.isArray(payload.items)) {
        items = payload.items;
      } else {
        // Fallback: try to find first array property
        const maybeArray = Object.values(payload).find(v => Array.isArray(v));
        items = Array.isArray(maybeArray) ? (maybeArray as any[]) : [];
      }
      setProducts(items || []);
    } catch (err) {
      console.error('Error fetching products:', err);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-96">
        <Loader2 className="h-8 w-8 animate-spin text-blue-600" />
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold">
            {isAdmin ? 'Product Management' : 'Fixed Deposit Products'}
          </h1>
          <p className="text-gray-600 mt-2">
            {isAdmin 
              ? 'Create and manage fixed deposit products' 
              : 'Browse our fixed deposit products and invest today'}
          </p>
        </div>
      </div>

      {isAdmin ? (
        <AdminProductsView 
          products={products} 
          loading={loading} 
          onRefresh={fetchProducts}
        />
      ) : (
        <CustomerProductsView 
          products={products} 
          loading={loading}
        />
      )}
    </div>
  );
}
