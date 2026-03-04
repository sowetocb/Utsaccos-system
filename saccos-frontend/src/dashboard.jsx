import { useEffect, useState } from "react";
import api from "./api/axios";

function Dashboard() {
  const [members, setMembers] = useState([]);
  const [loading, setLoading] = useState(true);
  const fullName = localStorage.getItem("fullName"); // Logged-in user

  useEffect(() => {
    const fetchMembers = async () => {
      try {
        const response = await api.get("/members");
        setMembers(response.data);
      } catch (error) {
        console.error("Error fetching members", error);
      } finally {
        setLoading(false);
      }
    };

    fetchMembers();
  }, []);

  // Calculate stats
  const totalMembers = members.length;
  const activeMembers = members.filter(m => m.status?.isActive).length;

  return (
    <div style={{ padding: "40px", fontFamily: "Arial, sans-serif" }}>
      <h1>Welcome, {fullName}</h1>

      {/* Quick Stats */}
      <div style={{ display: "flex", gap: "20px", margin: "20px 0" }}>
        <div style={{ padding: "20px", border: "1px solid #ccc", borderRadius: "8px" }}>
          <h3>Total Members</h3>
          <p style={{ fontSize: "24px", fontWeight: "bold" }}>{totalMembers}</p>
        </div>
        <div style={{ padding: "20px", border: "1px solid #ccc", borderRadius: "8px" }}>
          <h3>Active Members</h3>
          <p style={{ fontSize: "24px", fontWeight: "bold" }}>{activeMembers}</p>
        </div>
      </div>

      {/* Members Table */}
      {loading ? (
        <p>Loading members...</p>
      ) : members.length === 0 ? (
        <p>No members found</p>
      ) : (
        <table
          border="1"
          cellPadding="10"
          style={{ borderCollapse: "collapse", width: "100%" }}
        >
          <thead style={{ backgroundColor: "#f0f0f0" }}>
            <tr>
                <th>User name</th>
              <th>Full Name</th>
              <th>Member Number</th>
              <th>ID Number</th>
              <th>Email</th>
              <th>Phone</th>
              <th>Date of Birth</th>
              <th>Join Date</th>
              <th>Status</th>
            </tr>
          </thead>
          <tbody>
            {members.map((member) => (
              <tr key={member.memberNumber}>
                     <td>{member.username}</td>
                   <td>{member.firstName} {member.lastName}</td>
                  <td>{member.memberNumber}</td>
                  <td>{member.idNumber}</td>
                 <td>{member.email}</td>
                 <td>{member.phone}</td>
                 <td>{member.dateOfBirth}</td>
                 <td>{member.joinDate}</td>
                 <td>{member.status?.statusName || "N/A"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

export default Dashboard;