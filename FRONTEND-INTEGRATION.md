# 🔗 Frontend Integration Guide

## 🌐 CORS Configuration

O backend está configurado para aceitar requisições dos seguintes frontends:

### ✅ **Origens Permitidas:**
- `http://localhost:3000` (React/Next.js padrão)
- `http://localhost:3001` (React alternativo)
- `http://localhost:4200` (Angular padrão)
- `http://localhost:8081` (Vue.js/outros)
- `http://127.0.0.1:3000` (React via IP)
- `http://127.0.0.1:4200` (Angular via IP)

### 🔧 **Métodos HTTP Permitidos:**
- `GET`, `POST`, `PUT`, `DELETE`, `OPTIONS`, `PATCH`

### 📋 **Headers Expostos:**
- `X-Total-Count` - Total de itens (para paginação)
- `X-Total-Pages` - Total de páginas
- `Content-Range` - Range de conteúdo
- `Authorization` - Token de autenticação

## 📖 API Documentation

### **Swagger UI:**
- **URL:** http://localhost:8080/swagger-ui.html
- **API Docs:** http://localhost:8080/api-docs
- **OpenAPI JSON:** http://localhost:8080/api-docs.json

## 🚀 Como Integrar com Frontend

### **React/Next.js Example:**

```javascript
// api/tourService.js
const API_BASE_URL = 'http://localhost:8080/api/v1';

class TourService {
  async getAllTours(page = 0, size = 10) {
    const response = await fetch(
      `${API_BASE_URL}/tours?page=${page}&size=${size}`,
      {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include', // Para cookies se necessário
      }
    );
    
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    
    return response.json();
  }

  async createTour(tourData) {
    const response = await fetch(`${API_BASE_URL}/tours`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(tourData),
      credentials: 'include',
    });
    
    if (!response.ok) {
      const errorData = await response.json();
      throw new Error(errorData.message || 'Failed to create tour');
    }
    
    return response.json();
  }

  async updateTour(id, tourData) {
    const response = await fetch(`${API_BASE_URL}/tours/${id}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(tourData),
      credentials: 'include',
    });
    
    return response.json();
  }

  async deleteTour(id) {
    const response = await fetch(`${API_BASE_URL}/tours/${id}`, {
      method: 'DELETE',
      credentials: 'include',
    });
    
    return response.ok;
  }

  async searchTours(filters) {
    const params = new URLSearchParams();
    
    Object.keys(filters).forEach(key => {
      if (filters[key] !== undefined && filters[key] !== '') {
        params.append(key, filters[key]);
      }
    });
    
    const response = await fetch(
      `${API_BASE_URL}/tours/search?${params.toString()}`
    );
    
    return response.json();
  }
}

export default new TourService();
```

### **Angular Example:**

```typescript
// tour.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Tour {
  id?: string;
  name: string;
  description: string;
  destination: string;
  price: number;
  durationDays: number;
  maxParticipants: number;
  status: 'ACTIVE' | 'INACTIVE' | 'CANCELLED' | 'FULL';
  imageUrl?: string;
}

export interface PaginatedResponse<T> {
  content: T[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
  };
}

@Injectable({
  providedIn: 'root'
})
export class TourService {
  private apiUrl = 'http://localhost:8080/api/v1/tours';

  constructor(private http: HttpClient) {}

  getAllTours(page: number = 0, size: number = 10): Observable<PaginatedResponse<Tour>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<PaginatedResponse<Tour>>(this.apiUrl, { params });
  }

  getTourById(id: string): Observable<Tour> {
    return this.http.get<Tour>(`${this.apiUrl}/${id}`);
  }

  createTour(tour: Tour): Observable<Tour> {
    return this.http.post<Tour>(this.apiUrl, tour);
  }

  updateTour(id: string, tour: Tour): Observable<Tour> {
    return this.http.put<Tour>(`${this.apiUrl}/${id}`, tour);
  }

  deleteTour(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  searchTours(filters: any, page: number = 0, size: number = 10): Observable<PaginatedResponse<Tour>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    Object.keys(filters).forEach(key => {
      if (filters[key] !== undefined && filters[key] !== '') {
        params = params.set(key, filters[key]);
      }
    });

    return this.http.get<PaginatedResponse<Tour>>(`${this.apiUrl}/search`, { params });
  }
}
```

### **Vue.js Example:**

```javascript
// tourService.js
import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/v1';

// Configuração global do Axios
const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Para cookies
});

// Interceptor para tratamento de erros
apiClient.interceptors.response.use(
  response => response,
  error => {
    console.error('API Error:', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

export const tourService = {
  async getAllTours(page = 0, size = 10) {
    const response = await apiClient.get('/tours', {
      params: { page, size }
    });
    return response.data;
  },

  async getTourById(id) {
    const response = await apiClient.get(`/tours/${id}`);
    return response.data;
  },

  async createTour(tourData) {
    const response = await apiClient.post('/tours', tourData);
    return response.data;
  },

  async updateTour(id, tourData) {
    const response = await apiClient.put(`/tours/${id}`, tourData);
    return response.data;
  },

  async deleteTour(id) {
    await apiClient.delete(`/tours/${id}`);
    return true;
  },

  async searchTours(filters, page = 0, size = 10) {
    const params = { page, size, ...filters };
    const response = await apiClient.get('/tours/search', { params });
    return response.data;
  },

  async getToursByStatus(status, page = 0, size = 10) {
    const response = await apiClient.get(`/tours/status/${status}`, {
      params: { page, size }
    });
    return response.data;
  },

  async getPopularDestinations() {
    const response = await apiClient.get('/tours/destinations/popular');
    return response.data;
  }
};
```

## 📱 Response Format

### **Standard Response:**
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": "123e4567-e89b-12d3-a456-426614174000",
    "name": "Amazing Paris Tour",
    "description": "Explore the beautiful city of Paris",
    "destination": "Paris, France",
    "price": 1299.99,
    "durationDays": 7,
    "maxParticipants": 20,
    "status": "ACTIVE",
    "imageUrl": "https://example.com/paris.jpg",
    "createdAt": "2025-01-15T10:30:00",
    "updatedAt": "2025-01-15T10:30:00"
  },
  "timestamp": "2025-01-15T10:30:00",
  "status": 200
}
```

### **Paginated Response:**
```json
{
  "content": [
    {
      "id": "123e4567-e89b-12d3-a456-426614174000",
      "name": "Amazing Paris Tour",
      "destination": "Paris, France",
      "price": 1299.99,
      "durationDays": 7,
      "status": "ACTIVE",
      "imageUrl": "https://example.com/paris.jpg"
    }
  ],
  "pagination": {
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "first": true,
    "last": false,
    "hasNext": true,
    "hasPrevious": false,
    "numberOfElements": 10,
    "empty": false
  }
}
```

### **Error Response:**
```json
{
  "success": false,
  "message": "Operation failed",
  "error": {
    "code": "TOUR_NOT_FOUND",
    "message": "Tour not found with ID: 123",
    "details": null
  },
  "timestamp": "2025-01-15T10:30:00",
  "status": 404
}
```

### **Validation Error Response:**
```json
{
  "success": false,
  "message": "Validation failed",
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid request data",
    "details": {
      "name": "Tour name is required",
      "price": "Price must be greater than 0"
    }
  },
  "timestamp": "2025-01-15T10:30:00",
  "status": 400
}
```

## 🔐 Authentication (Future)

### **JWT Bearer Token:**
```javascript
// Adicionar token nos headers
const token = localStorage.getItem('authToken');

const headers = {
  'Content-Type': 'application/json',
  'Authorization': `Bearer ${token}`
};
```

### **API Key Authentication:**
```javascript
// Adicionar API Key nos headers
const headers = {
  'Content-Type': 'application/json',
  'X-API-Key': 'your-api-key-here'
};
```

## 🛠️ Error Handling

### **Frontend Error Handler:**
```javascript
class ApiError extends Error {
  constructor(message, status, code, details) {
    super(message);
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

async function handleApiResponse(response) {
  if (!response.ok) {
    const errorData = await response.json();
    throw new ApiError(
      errorData.error?.message || 'An error occurred',
      response.status,
      errorData.error?.code,
      errorData.error?.details
    );
  }
  return response.json();
}

// Uso
try {
  const tour = await tourService.createTour(tourData);
  console.log('Tour created:', tour);
} catch (error) {
  if (error instanceof ApiError) {
    if (error.status === 400 && error.code === 'VALIDATION_ERROR') {
      // Tratar erros de validação
      console.log('Validation errors:', error.details);
    } else if (error.status === 404) {
      // Tratar recurso não encontrado
      console.log('Resource not found');
    }
  }
}
```

## 📋 TypeScript Interfaces

```typescript
// types/tour.ts
export interface Tour {
  id?: string;
  name: string;
  description: string;
  destination: string;
  price: number;
  durationDays: number;
  maxParticipants: number;
  status: TourStatus;
  imageUrl?: string;
  createdAt?: string;
  updatedAt?: string;
}

export enum TourStatus {
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  CANCELLED = 'CANCELLED',
  FULL = 'FULL'
}

export interface TourSearchFilters {
  name?: string;
  destination?: string;
  minPrice?: number;
  maxPrice?: number;
  minDays?: number;
  maxDays?: number;
  status?: TourStatus;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data?: T;
  error?: {
    code: string;
    message: string;
    details?: any;
  };
  timestamp: string;
  status: number;
}

export interface PaginatedResponse<T> {
  content: T[];
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
    hasNext: boolean;
    hasPrevious: boolean;
    numberOfElements: number;
    empty: boolean;
  };
}
```

## 🎨 Frontend Components Examples

### **React Component:**
```jsx
import React, { useState, useEffect } from 'react';
import tourService from '../services/tourService';

const TourList = () => {
  const [tours, setTours] = useState([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({});
  const [currentPage, setCurrentPage] = useState(0);

  useEffect(() => {
    loadTours();
  }, [currentPage]);

  const loadTours = async () => {
    try {
      setLoading(true);
      const response = await tourService.getAllTours(currentPage, 10);
      setTours(response.content);
      setPagination(response.pagination);
    } catch (error) {
      console.error('Error loading tours:', error);
    } finally {
      setLoading(false);
    }
  };

  const deleteTour = async (id) => {
    if (window.confirm('Are you sure you want to delete this tour?')) {
      try {
        await tourService.deleteTour(id);
        loadTours(); // Reload the list
      } catch (error) {
        console.error('Error deleting tour:', error);
      }
    }
  };

  if (loading) return <div>Loading...</div>;

  return (
    <div>
      <h2>Tours</h2>
      <div className="tour-grid">
        {tours.map(tour => (
          <div key={tour.id} className="tour-card">
            <h3>{tour.name}</h3>
            <p>{tour.destination}</p>
            <p>${tour.price}</p>
            <p>{tour.durationDays} days</p>
            <button onClick={() => deleteTour(tour.id)}>Delete</button>
          </div>
        ))}
      </div>
      
      {/* Pagination */}
      <div className="pagination">
        <button 
          disabled={pagination.first} 
          onClick={() => setCurrentPage(currentPage - 1)}
        >
          Previous
        </button>
        <span>Page {pagination.page + 1} of {pagination.totalPages}</span>
        <button 
          disabled={pagination.last} 
          onClick={() => setCurrentPage(currentPage + 1)}
        >
          Next
        </button>
      </div>
    </div>
  );
};

export default TourList;
```

## 🚦 Health Check

### **Check Backend Status:**
```javascript
// healthService.js
export const checkBackendHealth = async () => {
  try {
    const response = await fetch('http://localhost:8080/actuator/health');
    const health = await response.json();
    return health.status === 'UP';
  } catch (error) {
    console.error('Backend health check failed:', error);
    return false;
  }
};

// Use in app initialization
useEffect(() => {
  const checkHealth = async () => {
    const isHealthy = await checkBackendHealth();
    if (!isHealthy) {
      setBackendStatus('Backend is not available');
    }
  };
  
  checkHealth();
}, []);
```

## 🔄 Real-time Updates (Future)

Para implementar atualizações em tempo real no futuro:

### **Server-Sent Events (SSE):**
```javascript
// Real-time tour updates
const eventSource = new EventSource('http://localhost:8080/api/v1/tours/events');

eventSource.onmessage = (event) => {
  const tourUpdate = JSON.parse(event.data);
  // Update tour in your state
  updateTourInState(tourUpdate);
};
```

### **WebSocket Connection:**
```javascript
// WebSocket for real-time notifications
const ws = new WebSocket('ws://localhost:8080/ws/tours');

ws.onmessage = (event) => {
  const notification = JSON.parse(event.data);
  showNotification(notification);
};
```

## 📝 Environment Configuration

### **.env file for React:**
```bash
REACT_APP_API_BASE_URL=http://localhost:8080/api/v1
REACT_APP_WS_URL=ws://localhost:8080/ws
REACT_APP_APP_NAME=Tour Management
```

### **environment.ts for Angular:**
```typescript
export const environment = {
  production: false,
  apiBaseUrl: 'http://localhost:8080/api/v1',
  wsUrl: 'ws://localhost:8080/ws',
  appName: 'Tour Management'
};
```

---

## 🎯 **Quick Start Checklist:**

1. ✅ **Start Backend:** `./gradlew bootRun`
2. ✅ **Verify CORS:** Check browser console for CORS errors
3. ✅ **Test API:** Visit http://localhost:8080/swagger-ui.html
4. ✅ **Check Health:** http://localhost:8080/actuator/health
5. ✅ **Integrate Frontend:** Use examples above
6. ✅ **Handle Errors:** Implement error handling
7. ✅ **Add Loading States:** Show loading indicators
8. ✅ **Test Pagination:** Test with multiple pages

**Happy Coding! 🚀**