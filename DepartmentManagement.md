1. Requirement

We identify what HR needs to do:

HR should be able to:
- View departments
- Create department
- Edit department
- Activate/deactivate department
- Search/filter departments
- See employee count

Business logic

Then we document something like:

Create Department

1. Validate request
2. Normalize code/name
3. Check duplicate code
4. Check duplicate name
5. Set status = ACTIVE
6. Save department
7. Return response

For deactivation:

Deactivate Department

1. Find department
2. Check current status
3. Check whether active employees belong to it
4. If employees exist → reject
5. Otherwise → INACTIVE
6. Return updated department

API design
Then define the contract:
POST   /api/departments
GET    /api/departments
GET    /api/departments/{id}
PUT    /api/departments/{id}
PATCH  /api/departments/{id}/status

Department APIs we have

| API                              | Purpose                          | Status                         |
| -------------------------------- | -------------------------------- | ------------------------------ |
| `POST /departments`              | Create department                | ✅ Done                         |
| `GET /departments`               | Get all departments              | ✅ Done                         |
| `GET /departments/{id}`          | Get department by ID             | ✅ Done                         |
| `PUT /departments/{id}`          | Update department                | ✅ Done                         |
| `PATCH /departments/{id}/status` | Activate / deactivate department | ✅ Done                         |
| `DELETE /departments/{id}`       | Physical delete                  | ❌ Should not use in production |


What is actually pending?
1. Employee count by department
This should be added once the Employee module exists

2. Deactivation / soft-delete business rule

So I'd mark Department like this
Department Module
│
├── Create Department                 ✅
├── Get All Departments               ✅
├── Get Department By ID              ✅
├── Update Department                 ✅
├── Activate Department               ✅
├── Deactivate Department             ✅
│
├── Employee Count                    ⏳ Employee module dependency
├── Prevent deactivation with
│   active employees                  ⏳ Employee module dependency
│
├── Search / Filter                   🔜 Recommended
├── Pagination                        🔜 Recommended
└── Sorting                            🔜 Recommended

Final Department status
Here's exactly where I would put us:
| Feature                                    | Status                             |
| ------------------------------------------ | ---------------------------------- |
| Department entity                          | ✅ Complete                         |
| DB constraints                             | ✅ Complete                         |
| Status                                     | ✅ Complete                         |
| Create                                     | ✅ Complete                         |
| Get all                                    | ✅ Complete                         |
| Get by ID                                  | ✅ Complete                         |
| Filter by status                           | ✅ Complete, but merge GET mappings |
| Update                                     | ✅ Complete                         |
| Validation                                 | ✅ Complete                         |
| Duplicate handling                         | ✅ Complete                         |
| Authorization                              | ✅ Complete                         |
| Auditing                                   | ✅ Complete                         |
| Transactions                               | ✅ Complete                         |
| Swagger                                    | ✅ Complete                         |
| Hard DELETE                                | ❌ Remove                           |
| Employee count                             | ⏳ After Employee                   |
| Prevent deactivation with active employees | ⏳ After Employee                   |
| Department head                            | ⏳ After Employee                   |
| Search                                     | 🔜 Later                           |
| Pagination                                 | 🔜 Later                           |
| Sorting                                    | 🔜 Later                           |
| Automated tests                            | ⚠️ Verify/add                      |
